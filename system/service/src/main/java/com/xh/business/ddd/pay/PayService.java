package com.xh.business.ddd.pay;

import com.xh.business.domain.model.ApiProductOrder;
import com.xh.business.domain.resp.order.ProductOrderVO;
import com.xh.business.domain.resp.user.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public interface PayService {

    /**
     * 获取产品订单
     * 获取订单
     *
     * @param productId 产品id
     * @param loginUser 登录用户
     * @param payType   付款类型
     * @return {@link ProductOrderVO}
     */
    ProductOrderVO getProductOrder(Long productId, UserVO loginUser, String payType);

    /**
     * 保存产品订单
     *
     * @param productId 产品id
     * @param loginUser 登录用户
     * @return {@link ProductOrderVO}
     */
    ProductOrderVO saveProductOrder(Long productId, UserVO loginUser);

    /**
     * 处理超时订单
     * 检查订单状态(微信查单接口)
     *
     * @param productOrder 产品订单
     */
    void processingTimedOutOrders(ApiProductOrder productOrder);

    /**
     * 付款通知
     * 处理付款通知
     *
     * @param notifyData 通知数据
     * @param request    要求
     * @return {@link String}
     */
    String doPaymentNotify(String notifyData, HttpServletRequest request);

}
