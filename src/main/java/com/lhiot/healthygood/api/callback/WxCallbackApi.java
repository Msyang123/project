package com.lhiot.healthygood.api.callback;

import com.leon.microx.util.DateTime;
import com.leon.microx.web.result.Tips;
import com.leon.microx.web.session.Sessions;
import com.lhiot.healthygood.domain.customplan.CustomOrder;
import com.lhiot.healthygood.feign.BaseUserServerFeign;
import com.lhiot.healthygood.feign.OrderServiceFeign;
import com.lhiot.healthygood.feign.PaymentServiceFeign;
import com.lhiot.healthygood.feign.model.BalanceOperationParam;
import com.lhiot.healthygood.feign.model.Payed;
import com.lhiot.healthygood.feign.type.ApplicationType;
import com.lhiot.healthygood.feign.type.OperationStatus;
import com.lhiot.healthygood.mapper.customplan.CustomOrderMapper;
import com.lhiot.healthygood.type.CustomOrderStatus;
import com.lhiot.healthygood.util.ConvertRequestToMap;
import com.lhiot.healthygood.util.FeginResponseTools;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Api(description = "微信支付回调接口")
@Slf4j
@RestController
@RequestMapping("/callback/wx-pay")
public class WxCallbackApi {
    private final static String PREFIX_REDIS = "lhiot:healthy_good:wx_pay_callback_msg";
    private final PaymentServiceFeign paymentServiceFeign;
    private final OrderServiceFeign orderServiceFeign;
    private final BaseUserServerFeign baseUserServerFeign;
    private final CustomOrderMapper customOrderMapper;
    private final RedissonClient redissonClient;

    @Autowired
    public WxCallbackApi(PaymentServiceFeign paymentServiceFeign, OrderServiceFeign orderServiceFeign, BaseUserServerFeign baseUserServerFeign, CustomOrderMapper customOrderMapper, RedissonClient redissonClient) {
        this.paymentServiceFeign = paymentServiceFeign;
        this.orderServiceFeign = orderServiceFeign;
        this.baseUserServerFeign = baseUserServerFeign;
        this.customOrderMapper = customOrderMapper;
        this.redissonClient = redissonClient;
    }

    @Sessions.Uncheck
    @PostMapping("/orders")
    @ApiOperation("订单支付微信回调-后端回调处理")
    public ResponseEntity<String> wxPayOrderCallback(HttpServletRequest request) {
        Map<String, String> parameters = ConvertRequestToMap.convertRequestXmlFormatToMap(request);

        Tips wxVerifyTips = wxVerify(parameters);
        if (wxVerifyTips.err()) {
            //TODO 基础支付服务如果有取消或者关单接口就调用
            log.error("订单支付微信回调参数验签失败:{},{}", parameters, wxVerifyTips);
            return ResponseEntity.badRequest().body(wxVerifyTips.getMessage());
        }

        //传递支付单号(parameters.get("outTradeNo")) 调用订单中心修改为成功支付
        Payed payed = new Payed();
        payed.setBankType(parameters.get("bank_type"));
        payed.setPayAt(DateTime.date(parameters.get("time_end"), "yyyyMMddHHmmss"));
        payed.setPayId(parameters.get("out_trade_no"));
        payed.setTradeId(parameters.get("transaction_id"));
        Tips tips = FeginResponseTools.convertResponse(orderServiceFeign.updateOrderToPayed(parameters.get("attach"), payed));

        if (tips.err()) {
            log.error("调用订单修改支付失败:{}", tips);
            return ResponseEntity.badRequest().body("调用订单修改支付失败");
        }
        //TODO 本地mq延迟到配送时间前一小时发送海鼎

        //返回成功处理给微信
        return ResponseEntity.ok("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<xml><return_code><![CDATA[SUCCESS]]></return_code>"
                + "<return_msg><![CDATA[OK]]></return_msg></xml>");
    }

    @Sessions.Uncheck
    @PostMapping("/recharge")
    @ApiOperation("充值支付微信回调-后端回调处理")
    public ResponseEntity<String> wxPayRechargeCallback(HttpServletRequest request) {
        Map<String, String> parameters = ConvertRequestToMap.convertRequestXmlFormatToMap(request);
        Tips wxVerifyTips = wxVerify(parameters);
        if (wxVerifyTips.err()) {
            //TODO 基础支付服务如果有取消或者关单接口就调用
            log.error("充值微信回调参数验签失败{},{}", parameters, wxVerifyTips);
            return ResponseEntity.badRequest().body(wxVerifyTips.getMessage());
        }

        Long userId = Long.valueOf(parameters.get("attach"));
        //给用户加鲜果币
        BalanceOperationParam balanceOperationParam = new BalanceOperationParam();
        balanceOperationParam.setApplicationType(ApplicationType.HEALTH_GOOD);
        balanceOperationParam.setMoney(Long.valueOf(parameters.get("total_fee")));
        balanceOperationParam.setOperation(OperationStatus.ADD);
        balanceOperationParam.setSourceId(parameters.get("out_trade_no"));
        balanceOperationParam.setSourceType("recharge");
        Tips tips = FeginResponseTools.convertResponse(baseUserServerFeign.userBalanceOperation(userId, balanceOperationParam));
        if (tips.err()) {
            log.error("充值微信回调给用户加鲜果币失败{},{}", userId, balanceOperationParam);
            return ResponseEntity.badRequest().body("微信回调参数验签失败");
        }
        //修改支付日志
        Payed payed = new Payed();
        payed.setBankType(parameters.get("bank_type"));
        payed.setPayAt(DateTime.date(parameters.get("time_end"), "yyyyMMddHHmmss"));
        payed.setPayId(parameters.get("out_trade_no"));
        payed.setTradeId(parameters.get("transaction_id"));
        paymentServiceFeign.completed(parameters.get("out_trade_no"), payed);
        return ResponseEntity.ok("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<xml><return_code><![CDATA[SUCCESS]]></return_code>"
                + "<return_msg><![CDATA[OK]]></return_msg></xml>");
    }

    @Sessions.Uncheck
    @PostMapping("/activity")
    @ApiOperation("定制计划支付微信回调-后端回调处理")
    public ResponseEntity<String> wxPayCustomPlanCallback(HttpServletRequest request) {
        Map<String, String> parameters = ConvertRequestToMap.convertRequestXmlFormatToMap(request);
        //调用基础服务验证参数签名是否正确
        Tips wxVerifyTips = wxVerify(parameters);
        if (wxVerifyTips.err()) {
            //TODO 基础支付服务如果有取消或者关单接口就调用
            log.error("定制计划支付微信回调参数验签失败:{},{}", parameters, wxVerifyTips);
            return ResponseEntity.badRequest().body(wxVerifyTips.getMessage());
        }

        Payed payed = new Payed();
        payed.setBankType(parameters.get("bank_type"));
        payed.setPayAt(DateTime.date(parameters.get("time_end"), "yyyyMMddHHmmss"));
        payed.setPayId(parameters.get("out_trade_no"));
        payed.setTradeId(parameters.get("transaction_id"));
        Tips tips = FeginResponseTools.convertResponse(paymentServiceFeign.completed(parameters.get("out_trade_no"), payed));
        if (tips.err()) {
            log.error("调用支付日志修改失败:{}", tips);
            return ResponseEntity.badRequest().body("调用支付日志修改失败");
        }
        //修改定制计划订单状态定制中
        String customOrderCode = parameters.get("attach");
        CustomOrder customOrder = new CustomOrder();
        customOrder.setCustomOrderCode(customOrderCode);
        customOrder.setStatus(CustomOrderStatus.CUSTOMING);//定制中
        log.info("定制计划支付微信回调-后端回调处理{}", customOrder);
        customOrderMapper.updateByCode(customOrder);
        return ResponseEntity.ok("success");
    }

    /**
     * 验证回调参数
     *
     * @param parameters
     * @return
     */
    private Tips wxVerify(Map<String, String> parameters) {

        String outTradeNo = parameters.get("out_trade_no");
        //幂等处理
        RMapCache<String, String> cache = redissonClient.getMapCache(PREFIX_REDIS);
        String transactionId = cache.get(parameters.get("transaction_id"));//微信支付订单号
        //不为空，说明已经存在此回调，直接返回
        if (Objects.nonNull(transactionId)) {
            log.error("微信支付回调幂等处理存在重复发起调用，查询redis中{},{}", PREFIX_REDIS, transactionId);
            return Tips.warn("重复发起调用");
        }
        //缓存4个小时
        cache.put(parameters.get("transaction_id"), outTradeNo, 4, TimeUnit.HOURS);
        //调用基础服务验证参数签名是否正确
        Tips wxVerify = FeginResponseTools.convertResponse(paymentServiceFeign.wxVerify(outTradeNo, parameters));
        return wxVerify;
    }

}