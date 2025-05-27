package com.xh.business.service;

import com.xh.business.domain.model.ApiProductInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author apple
* @description 针对表【api_product_info(产品信息)】的数据库操作Service
* @createDate 2025-05-26 16:55:21
*/
public interface ApiProductInfoService extends IService<ApiProductInfo> {

    /**
     * 有效产品信息
     * 校验
     *
     * @param add         是否为创建校验
     * @param productInfo 产品信息
     */
    void validProductInfo(ApiProductInfo productInfo, boolean add);

}
