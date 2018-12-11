package com.lhiot.healthygood.feign.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.Date;

/**
 * @author Leon (234239150@qq.com) created in 11:35 18.12.3
 */
@Data
@ApiModel(description = "支付成功后修改支付记录")
public class PayedModel {

    @NotBlank(message = "支付平台交易号不能为空")
    @ApiModelProperty(value = "支付平台交易号(第三方生成)", dataType = "String", required = true)
    private String tradeId;

    @ApiModelProperty(value = "付款方式", dataType = "String", required = true)
    private String bankType;

    @NotEmpty
    @ApiModelProperty(value = "付款时间", dataType = "Date", required = true)
    private Date payAt;
}
