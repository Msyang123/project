package com.lhiot.healthygood.mapper.customplan;

import com.lhiot.healthygood.domain.customplan.CustomPlanProduct;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
* Description:定制计划关联商品Mapper类
* @author zhangs
* @date 2018/11/26
*/
@Mapper
@Repository
public interface CustomPlanProductMapper {

    /**
     * Description:新增定制计划关联商品
     *
     * @param customPlanProduct
     * @return
    * @author zhangs
    * @date 2018/11/26 15:53:47
     * @date 2018/11/26 15:53:47
     */
    int create(CustomPlanProduct customPlanProduct);

    /**
    * Description:根据id修改定制计划关联商品
    *
    * @param customPlanProduct
    * @return
    * @author zhangs
    * @date 2018/11/26 15:53:47
    */
    int updateById(CustomPlanProduct customPlanProduct);

    /**
    * Description:根据ids删除定制计划关联商品
    *
    * @param ids
    * @return
    * @author zhangs
    * @date 2018/11/26 15:53:47
    */
    int deleteByIds(List<String> ids);

    /**
    * Description:根据id查找定制计划关联商品
    *
    * @param id
    * @return
    * @author zhangs
    * @date 2018/11/26 15:53:47
    */
    CustomPlanProduct selectById(Long id);

    List<CustomPlanProduct> findByPlanId(Long planId);

    /**
    * Description:查询定制计划关联商品列表
    *
    * @param customPlanProduct
    * @return
    * @author zhangs
    * @date 2018/11/26 15:53:47
    */
     List<CustomPlanProduct> pageCustomPlanProducts(CustomPlanProduct customPlanProduct);


    /**
    * Description: 查询定制计划关联商品总记录数
    *
    * @param customPlanProduct
    * @return
    * @author zhangs
    * @date 2018/11/26 15:53:47
    */
    long pageCustomPlanProductCounts(CustomPlanProduct customPlanProduct);

    /**
     * 新增定制商品集合
     *
     * @param customPlanProducts 定制商品集合
     * @return 定制商品id
     */
    int insertList(List<CustomPlanProduct> customPlanProducts);
}
