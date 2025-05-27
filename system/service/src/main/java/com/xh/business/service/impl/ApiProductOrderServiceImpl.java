package com.xh.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xh.business.domain.model.ApiProductOrder;
import com.xh.business.mapper.ApiProductOrderMapper;
import com.xh.business.service.ApiProductOrderService;
import org.springframework.stereotype.Service;

/**
* @author apple
* @description 针对表【api_product_order(商品订单)】的数据库操作Service实现
* @createDate 2025-05-26 16:55:21
*/
@Service
public class ApiProductOrderServiceImpl extends ServiceImpl<ApiProductOrderMapper, ApiProductOrder>
    implements ApiProductOrderService {

    @Override
    public boolean updateProductOrder(ApiProductOrder productOrder) {
        String formData = productOrder.getFormData();
        Long id = productOrder.getId();
        ApiProductOrder updateCodeUrl = new ApiProductOrder();
        updateCodeUrl.setFormData(formData);
        updateCodeUrl.setId(id);
        return this.updateById(updateCodeUrl);
    }

    @Override
    public boolean updateOrderStatusByOrderNo(String outTradeNo, String orderStatus) {
        ApiProductOrder productOrder = new ApiProductOrder();
        productOrder.setStatus(orderStatus);
        LambdaQueryWrapper<ApiProductOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ApiProductOrder::getOrderNo, outTradeNo);
        return this.update(productOrder, lambdaQueryWrapper);
    }

    @Override
    public ApiProductOrder getProductOrderByOutTradeNo(String outTradeNo) {
        LambdaQueryWrapper<ApiProductOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ApiProductOrder::getOrderNo, outTradeNo);
        return this.getOne(lambdaQueryWrapper);
    }
}




