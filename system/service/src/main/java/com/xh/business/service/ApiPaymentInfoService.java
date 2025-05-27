package com.xh.business.service;

import com.xh.business.domain.dto.pay.PaymentInfoDTO;
import com.xh.business.domain.model.ApiPaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author apple
* @description 针对表【api_payment_info(付款信息)】的数据库操作Service
* @createDate 2025-05-26 16:55:21
*/
public interface ApiPaymentInfoService extends IService<ApiPaymentInfo> {

    /**
     * 创建付款信息
     *
     * @param paymentInfoDTO 付款信息dto
     * @return boolean
     */
    boolean createPaymentInfo(PaymentInfoDTO paymentInfoDTO);

}
