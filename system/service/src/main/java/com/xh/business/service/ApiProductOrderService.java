package com.xh.business.service;

import com.xh.business.domain.model.ApiProductOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author apple
* @description 针对表【api_product_order(商品订单)】的数据库操作Service
* @createDate 2025-05-26 16:55:21
*/
public interface ApiProductOrderService extends IService<ApiProductOrder> {

    /**
     * 更新产品订单
     *
     * @param productOrder 产品订单
     * @return boolean
     */
    boolean updateProductOrder(ApiProductOrder productOrder);

    /**
     * 按订单号更新订单状态
     *
     * @param outTradeNo  订单号
     * @param orderStatus 订单状态
     * @return boolean
     */
    boolean updateOrderStatusByOrderNo(String outTradeNo, String orderStatus);

    /**
     * 通过out trade no获得产品订单
     * 获取产品订单状态
     *
     * @param outTradeNo 外贸编号
     * @return {@link String}
     */
    ApiProductOrder getProductOrderByOutTradeNo(String outTradeNo);

}
