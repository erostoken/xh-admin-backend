package com.xh.business.ddd.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xh.business.ddd.pay.PayService;
import static com.xh.business.domain.enums.PayTypeStatusEnum.ALIPAY;
import static com.xh.business.domain.enums.PayTypeStatusEnum.WX;
import com.xh.business.domain.enums.PaymentStatusEnum;
import com.xh.business.domain.enums.ProductTypeStatusEnum;
import com.xh.business.domain.model.ApiProductInfo;
import com.xh.business.domain.model.ApiProductOrder;
import com.xh.business.domain.model.ApiRechargeActivity;
import com.xh.business.domain.resp.order.ProductOrderVO;
import com.xh.business.domain.resp.user.UserVO;
import com.xh.business.service.ApiProductOrderService;
import com.xh.business.service.ApiRechargeActivityService;
import com.xh.business.service.impl.ApiProductInfoServiceImpl;
import com.xh.business.utils.BusinessException;
import com.xh.business.utils.ErrorCode;
import com.xh.business.utils.RedissonLockUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @Author: QiMu
 * @Date: 2023年08月25日 22:22
 * @Version: 1.0
 * @Description:
 */
@Slf4j
@Service
public class OrderService {

    @Resource
    private ApiProductOrderService productOrderService;
    @Resource
    private List<PayService> productOrderServices;
    @Resource
    private ApiRechargeActivityService rechargeActivityService;
    @Resource
    private ApiProductInfoServiceImpl productInfoService;
    @Resource
    private RedissonLockUtil redissonLockUtil;
    
    /**
     * 处理订单通知
     *
     * @param notifyData 通知数据
     * @param request    要求
     * @return {@link String}
     */
    public String doOrderNotify(String notifyData, HttpServletRequest request) {
        String payType;
        if (notifyData.startsWith("gmt_create=") && notifyData.contains("gmt_create") && notifyData.contains("sign_type") && notifyData.contains("notify_type")) {
            payType = ALIPAY.getValue();
        } else {
            payType = WX.getValue();
        }
        return this.getProductOrderServiceByPayType(payType).doPaymentNotify(notifyData, request);
    }

    /**
     * 按付费类型获取产品订单服务
     *
     * @param payType 付款类型
     * @return {@link PayService}
     */
    public PayService getProductOrderServiceByPayType(String payType){
        return productOrderServices.stream()
                .filter(s -> {
                    Qualifier qualifierAnnotation = s.getClass().getAnnotation(Qualifier.class);
                    return qualifierAnnotation != null && qualifierAnnotation.value().equals(payType);
                })
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "暂无该支付方式"));
    }

    /**
     * 按付款类型创建订单
     *
     * @param productId 产品id
     * @param payType   付款类型
     * @param loginUser 登录用户
     * @return {@link ProductOrderVO}
     */
    public ProductOrderVO createOrderByPayType(Long productId, String payType, UserVO loginUser) {
        // 按付费类型获取产品订单服务Bean
        PayService payService = getProductOrderServiceByPayType(payType);
        String redissonLock = ("getOrder_" + loginUser.getUserAccount()).intern();

        ProductOrderVO getProductOrderVO = redissonLockUtil.redissonDistributedLocks(redissonLock, () -> {
            // 订单存在就返回不再新创建
            return payService.getProductOrder(productId, loginUser, payType);
        });
        if (getProductOrderVO != null) {
            return getProductOrderVO;
        }
        redissonLock = ("createOrder_" + loginUser.getUserAccount()).intern();
        // 分布式锁工具
        return redissonLockUtil.redissonDistributedLocks(redissonLock, () -> {
            // 检查是否购买充值活动
            checkBuyRechargeActivity(loginUser.getId(), productId);
            // 保存订单,返回vo信息
            return payService.saveProductOrder(productId, loginUser);
        });
    }
    
    /**
     * 检查购买充值活动
     *
     * @param userId    用户id
     * @param productId 产品订单id
     */
    private void checkBuyRechargeActivity(Long userId, Long productId) {
        ApiProductInfo productInfo = productInfoService.getById(productId);
        if (productInfo.getProductType().equals(ProductTypeStatusEnum.RECHARGE_ACTIVITY.getValue())) {
            LambdaQueryWrapper<ApiProductOrder> orderLambdaQueryWrapper = new LambdaQueryWrapper<>();
            orderLambdaQueryWrapper.eq(ApiProductOrder::getUserId, userId);
            orderLambdaQueryWrapper.eq(ApiProductOrder::getProductId, productId);
            orderLambdaQueryWrapper.eq(ApiProductOrder::getStatus, PaymentStatusEnum.NOTPAY.getValue());
            orderLambdaQueryWrapper.or().eq(ApiProductOrder::getStatus, PaymentStatusEnum.SUCCESS.getValue());

            long orderCount = productOrderService.count(orderLambdaQueryWrapper);
            if (orderCount > 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "该商品只能购买一次，请查看是否已经创建了该订单，或者挑选其他商品吧！");
            }
            LambdaQueryWrapper<ApiRechargeActivity> activityLambdaQueryWrapper = new LambdaQueryWrapper<>();
            activityLambdaQueryWrapper.eq(ApiRechargeActivity::getUserId, userId);
            activityLambdaQueryWrapper.eq(ApiRechargeActivity::getProductId, productId);
            long count = rechargeActivityService.count(activityLambdaQueryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "该商品只能购买一次，请查看是否已经创建了该订单，或者挑选其他商品吧！！");
            }
        }
    }

    /**
     * 按时间获得未支付订单
     *
     * @param minutes 分钟
     * @param remove  是否是删除
     * @param payType 付款类型
     * @return {@link List}<{@link ApiProductOrder}>
     */
    List<ApiProductOrder> getNoPayOrderByDuration(int minutes, Boolean remove, String payType) {
        Instant instant = Instant.now().minus(Duration.ofMinutes(minutes));
        LambdaQueryWrapper<ApiProductOrder> productOrderLambdaQueryWrapper = new LambdaQueryWrapper<>();
        productOrderLambdaQueryWrapper.eq(ApiProductOrder::getStatus, PaymentStatusEnum.NOTPAY.getValue());
        if (StringUtils.isNotBlank(payType)) {
            productOrderLambdaQueryWrapper.eq(ApiProductOrder::getPayType, payType);
        }
        // 删除
        if (remove) {
            productOrderLambdaQueryWrapper.or().eq(ApiProductOrder::getStatus, PaymentStatusEnum.CLOSED.getValue());
        }
        productOrderLambdaQueryWrapper.and(p -> p.le(ApiProductOrder::getCreateTime, instant));
        return productOrderService.list(productOrderLambdaQueryWrapper);
    }
}
