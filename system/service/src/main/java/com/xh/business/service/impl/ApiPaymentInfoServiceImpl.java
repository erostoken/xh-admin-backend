package com.xh.business.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryV3Result;
import com.xh.business.domain.dto.pay.PaymentInfoDTO;
import com.xh.business.domain.model.ApiPaymentInfo;
import com.xh.business.mapper.ApiPaymentInfoMapper;
import com.xh.business.service.ApiPaymentInfoService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
* @author apple
* @description 针对表【api_payment_info(付款信息)】的数据库操作Service实现
* @createDate 2025-05-26 16:55:21
*/
@Service
public class ApiPaymentInfoServiceImpl extends ServiceImpl<ApiPaymentInfoMapper, ApiPaymentInfo>
    implements ApiPaymentInfoService {

    /**
     * 创建付款信息
     *
     * @param paymentInfoDTO 付款信息dto
     * @return boolean
     */
    @Override
    public boolean createPaymentInfo(PaymentInfoDTO paymentInfoDTO) {
        String transactionId = paymentInfoDTO.getTransactionId();
        String tradeType = paymentInfoDTO.getTradeType();
        String tradeState = paymentInfoDTO.getTradeState();
        String tradeStateDesc = paymentInfoDTO.getTradeStateDesc();
        String successTime = paymentInfoDTO.getSuccessTime();
        WxPayOrderQueryV3Result.Payer payer = paymentInfoDTO.getPayer();
        WxPayOrderQueryV3Result.Amount amount = paymentInfoDTO.getAmount();

        ApiPaymentInfo paymentInfo = new ApiPaymentInfo();
        paymentInfo.setOrderNo(paymentInfoDTO.getOutTradeNo());
        paymentInfo.setTransactionId(transactionId);
        paymentInfo.setTradeType(tradeType);
        paymentInfo.setTradeState(tradeState);
        if (StringUtils.isNotBlank(successTime)) {
            paymentInfo.setSuccessTime(successTime);
        }
        paymentInfo.setOpenid(payer.getOpenid());
        paymentInfo.setPayerTotal(amount.getPayerTotal());
        paymentInfo.setCurrency(amount.getCurrency());
        paymentInfo.setPayerCurrency(amount.getPayerCurrency());
        paymentInfo.setTotal(amount.getTotal());
        paymentInfo.setTradeStateDesc(tradeStateDesc);
        paymentInfo.setContent(JSONUtil.toJsonStr(paymentInfoDTO));
        return this.save(paymentInfo);
    }

}




