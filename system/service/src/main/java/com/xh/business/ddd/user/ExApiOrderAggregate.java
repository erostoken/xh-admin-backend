package com.xh.business.ddd.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xh.business.ddd.pay.PayService;
import com.xh.business.domain.enums.PaymentStatusEnum;
import com.xh.business.domain.enums.ProductTypeStatusEnum;
import com.xh.business.domain.model.ApiProductInfo;
import com.xh.business.domain.model.ApiProductOrder;
import com.xh.business.domain.model.ApiRechargeActivity;
import com.xh.business.domain.resp.order.ProductOrderVO;
import com.xh.business.domain.resp.user.UserVO;
import com.xh.business.service.ApiUserService;
import com.xh.business.utils.BusinessException;
import com.xh.business.utils.ErrorCode;
import com.xh.business.utils.RedissonLockUtil;
import com.xh.common.core.dto.ExUserInfoDTO;
import com.xh.common.core.utils.LoginUtil;
import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author xuwentao
 * @version 1.0
 * @date 2025/5/30
 * @description 外部API订单聚合层
 */
@Slf4j
@Component
public class ExApiOrderAggregate {

    @Resource
    private RedissonLockUtil redissonLockUtil;

    @Resource
    private List<PayService> payServiceList;
    @Resource
    private ApiUserService apiUserService;

    /**
     * 按付费类型获取产品订单服务
     *
     * @param payType 付款类型
     * @return {@link PayService}
     */
    public PayService getProductOrderServiceByPayType(String payType){
        return payServiceList.stream()
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
     * @return {@link ProductOrderVO}
     */
    public ProductOrderVO createProductByPayType(Long productId, String payType) {
        UserVO loginUser = apiUserService.getUser();

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
            // 保存订单,返回vo信息
            return payService.saveProductOrder(productId, loginUser);
        });
    }
}
