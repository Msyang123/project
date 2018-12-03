package com.lhiot.healthygood.api.advertisement;

import com.leon.microx.web.result.Pages;
import com.leon.microx.web.result.Tips;
import com.leon.microx.web.session.Sessions;
import com.lhiot.healthygood.feign.BaseDataServiceFeign;
import com.lhiot.healthygood.feign.model.Advertisement;
import com.lhiot.healthygood.feign.model.AdvertisementParam;
import com.lhiot.healthygood.util.FeginResponseTools;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Api(description = "广告类接口")
@Slf4j
@RestController
public class AdvertisementApi {

    private final BaseDataServiceFeign baseDataServiceFeign;

    @Autowired
    public AdvertisementApi(BaseDataServiceFeign baseDataServiceFeign) {
        this.baseDataServiceFeign = baseDataServiceFeign;
    }

    @Sessions.Uncheck
    @ApiOperation(value = "根据位置编码查询广告列表")
    @PostMapping("/advertisements/position")
    ResponseEntity searchAdvertisementPages(@Valid @RequestParam Long code,@RequestParam Integer pages,@RequestParam Integer rows){
        AdvertisementParam advertisementParam = new AdvertisementParam();
        advertisementParam.setPositionId(code);
        advertisementParam.setPage(pages);
        advertisementParam.setRows(rows);
        ResponseEntity<Pages<Advertisement>> advertisements = baseDataServiceFeign.searchAdvertisementPages(advertisementParam);
        Tips tips = FeginResponseTools.convertResponse(advertisements);
        if (tips.err()){
            return ResponseEntity.badRequest().body(tips);
        }
        return ResponseEntity.ok(advertisements.getBody().getArray());

    }
}
