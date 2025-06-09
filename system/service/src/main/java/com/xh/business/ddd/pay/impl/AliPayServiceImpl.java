package com.xh.business.ddd.pay.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.domain.AlipayTradeCloseModel;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryV3Result;
import com.ijpay.alipay.AliPayApi;
import com.ijpay.alipay.AliPayApiConfigKit;
import com.xh.business.config.AliPayAccountConfig;
import com.xh.business.config.EmailConfig;
import com.xh.business.ddd.pay.PayService;
import static com.xh.business.domain.constant.PayConstant.ORDER_PREFIX;
import static com.xh.business.domain.constant.PayConstant.RESPONSE_CODE_SUCCESS;
import static com.xh.business.domain.constant.PayConstant.TRADE_SUCCESS;
import com.xh.business.domain.constant.PointsConstant;
import com.xh.business.domain.dto.alipay.AliPayAsyncRespDTO;
import com.xh.business.domain.dto.pay.PaymentInfoDTO;
import com.xh.business.domain.enums.AlipayTradeStatusEnum;
import static com.xh.business.domain.enums.PaymentStatusEnum.SUCCESS;
import static com.xh.business.domain.enums.PayTypeStatusEnum.ALIPAY;
import static com.xh.business.domain.enums.PaymentStatusEnum.CLOSED;
import static com.xh.business.domain.enums.PaymentStatusEnum.NOTPAY;
import com.xh.business.domain.model.ApiProductInfo;
import com.xh.business.domain.model.ApiProductOrder;
import com.xh.business.domain.model.ApiRechargeActivity;
import com.xh.business.domain.model.ApiUser;
import com.xh.business.domain.resp.order.ProductOrderVO;
import com.xh.business.domain.resp.user.UserVO;
import com.xh.business.service.ApiPaymentInfoService;
import com.xh.business.service.ApiProductInfoService;
import com.xh.business.service.ApiRechargeActivityService;
import com.xh.business.service.ApiUserService;
import com.xh.business.service.impl.ApiProductOrderServiceImpl;
import com.xh.business.utils.BusinessException;
import com.xh.business.utils.EmailUtil;
import com.xh.business.utils.ErrorCode;
import com.xh.business.utils.RedissonLockUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * @Author: QiMu
 * @Date: 2023/08/23 03:18:35
 * @Version: 1.0
 * @Description: 接口顺序服务impl
 */
@Service
@Slf4j
@Qualifier("ALIPAY")
public class AliPayServiceImpl implements PayService {
    @Resource
    private EmailConfig emailConfig;
    @Resource
    private JavaMailSender mailSender;
    @Resource
    private AliPayAccountConfig aliPayAccountConfig;
    @Resource
    private ApiUserService userService;
    @Resource
    private ApiProductInfoService productInfoService;
    @Resource
    private ApiPaymentInfoService paymentInfoService;
    @Resource
    private RedissonLockUtil redissonLockUtil;
    @Resource
    private ApiRechargeActivityService rechargeActivityService;
    @Autowired
    private ApiProductOrderServiceImpl apiProductOrderServiceImpl;

    @Override
    public ProductOrderVO getProductOrder(Long productId, UserVO loginUser, String payType) {
        LambdaQueryWrapper<ApiProductOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ApiProductOrder::getProductId, productId);
        lambdaQueryWrapper.eq(ApiProductOrder::getStatus, NOTPAY.getValue());
        lambdaQueryWrapper.eq(ApiProductOrder::getPayType, payType);
        lambdaQueryWrapper.eq(ApiProductOrder::getUserId, loginUser.getId());
        ApiProductOrder oldOrder = apiProductOrderServiceImpl.getOne(lambdaQueryWrapper);
        if (oldOrder == null) {
            return null;
        }
        ProductOrderVO ProductOrderVO = new ProductOrderVO();
        BeanUtils.copyProperties(oldOrder, ProductOrderVO);
        ProductOrderVO.setProductInfo(JSONUtil.toBean(oldOrder.getProductInfo(), ApiProductInfo.class));
        ProductOrderVO.setAmount(oldOrder.getAmount().toString());
        return ProductOrderVO;
    }

    @Override
    public ProductOrderVO saveProductOrder(Long productId, UserVO loginUser) {
        ApiProductInfo productInfo = productInfoService.getById(productId);
        if (productInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "商品不存在");
        }
        // 5分钟有效期
        Date date = DateUtil.date(System.currentTimeMillis());
        Date expirationTime = DateUtil.offset(date, DateField.MINUTE, 5);
        String orderNo = ORDER_PREFIX + RandomUtil.randomNumbers(20);

        ApiProductOrder productOrder = new ApiProductOrder();
        productOrder.setUserId(loginUser.getId());
        productOrder.setOrderNo(orderNo);
        productOrder.setProductId(productInfo.getId());
        productOrder.setOrderName(productInfo.getName());
        productOrder.setAmount(productInfo.getAmount());
        productOrder.setStatus(NOTPAY.getValue());
        productOrder.setPayType(ALIPAY.getValue());
        productOrder.setExpirationTime(expirationTime);
        productOrder.setProductInfo(JSONUtil.toJsonPrettyStr(productInfo));
        productOrder.setAddPoints(productInfo.getAddPoints());

        boolean saveResult = apiProductOrderServiceImpl.save(productOrder);

        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(orderNo);
        model.setSubject(productInfo.getName());
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        // 金额四舍五入
        BigDecimal scaledAmount = new BigDecimal(productInfo.getAmount()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        model.setTotalAmount(String.valueOf(scaledAmount));
        model.setBody(productInfo.getDescription());

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setBizModel(model);
        request.setNotifyUrl(aliPayAccountConfig.getNotifyUrl());
        request.setReturnUrl(aliPayAccountConfig.getReturnUrl());

        try {
            AlipayTradePagePayResponse alipayTradePagePayResponse = AliPayApi.pageExecute(request);
            String payUrl = alipayTradePagePayResponse.getBody();
            productOrder.setFormData(payUrl);
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }

        boolean updateResult = apiProductOrderServiceImpl.updateProductOrder(productOrder);
        if (!updateResult & !saveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        // 构建vo
        ProductOrderVO ProductOrderVO = new ProductOrderVO();
        BeanUtils.copyProperties(productOrder, ProductOrderVO);
        ProductOrderVO.setProductInfo(productInfo);
        ProductOrderVO.setAmount(productInfo.getAmount().toString());
        return ProductOrderVO;
    }

    public void closedOrderByOrderNo(String outTradeNo) throws AlipayApiException {
        AlipayTradeCloseModel alipayTradeCloseModel = new AlipayTradeCloseModel();
        alipayTradeCloseModel.setOutTradeNo(outTradeNo);
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        request.setBizModel(alipayTradeCloseModel);
        AliPayApi.doExecute(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processingTimedOutOrders(ApiProductOrder productOrder) {
        String orderNo = productOrder.getOrderNo();
        try {
            // 查询订单
            AlipayTradeQueryModel alipayTradeQueryModel = new AlipayTradeQueryModel();
            alipayTradeQueryModel.setOutTradeNo(orderNo);
            AlipayTradeQueryResponse alipayTradeQueryResponse = AliPayApi.tradeQueryToResponse(alipayTradeQueryModel);

            // 本地创建了订单,但是用户没有扫码,支付宝端没有订单
            if (!alipayTradeQueryResponse.getCode().equals(RESPONSE_CODE_SUCCESS)) {
                // 更新本地订单状态
                apiProductOrderServiceImpl.updateOrderStatusByOrderNo(orderNo, CLOSED.getValue());
                log.info("超时订单{},更新成功", orderNo);
                return;
            }
            String tradeStatus = AlipayTradeStatusEnum.findByName(alipayTradeQueryResponse.getTradeStatus()).getPaymentStatusEnum().getValue();
            // 订单没有支付就关闭订单,更新本地订单状态
            if (tradeStatus.equals(NOTPAY.getValue()) || tradeStatus.equals(CLOSED.getValue())) {
                closedOrderByOrderNo(orderNo);
                apiProductOrderServiceImpl.updateOrderStatusByOrderNo(orderNo, CLOSED.getValue());
                log.info("超时订单{},关闭成功", orderNo);
                return;
            }
            if (tradeStatus.equals(SUCCESS.getValue())) {
                // 订单已支付更新商户端的订单状态
                boolean updateOrderStatus = apiProductOrderServiceImpl.updateOrderStatusByOrderNo(orderNo, SUCCESS.getValue());
                // 补发积分到用户钱包
                boolean addWalletBalance = userService.addWalletBalance(productOrder.getUserId(), productOrder.getAddPoints());
                // 保存支付记录
                PaymentInfoDTO paymentInfoDTO = new PaymentInfoDTO();
                paymentInfoDTO.setAppid(aliPayAccountConfig.getAppId());
                paymentInfoDTO.setOutTradeNo(alipayTradeQueryResponse.getOutTradeNo());
                paymentInfoDTO.setTransactionId(alipayTradeQueryResponse.getTradeNo());
                paymentInfoDTO.setTradeType("电脑网站支付");
                paymentInfoDTO.setTradeState(alipayTradeQueryResponse.getTradeStatus());
                paymentInfoDTO.setTradeStateDesc("支付成功");
                paymentInfoDTO.setSuccessTime(String.valueOf(alipayTradeQueryResponse.getSendPayDate()));
                WxPayOrderQueryV3Result.Payer payer = new WxPayOrderQueryV3Result.Payer();
                payer.setOpenid(alipayTradeQueryResponse.getBuyerOpenId());
                paymentInfoDTO.setPayer(payer);
                WxPayOrderQueryV3Result.Amount amount = new WxPayOrderQueryV3Result.Amount();
                amount.setTotal(new BigDecimal(alipayTradeQueryResponse.getTotalAmount()).multiply(new BigDecimal("100")).intValue());
                amount.setPayerTotal(new BigDecimal(alipayTradeQueryResponse.getReceiptAmount()).multiply(new BigDecimal("100")).intValue());
                amount.setCurrency(alipayTradeQueryResponse.getPayCurrency());
                amount.setPayerCurrency(alipayTradeQueryResponse.getPayCurrency());
                paymentInfoDTO.setAmount(amount);
                boolean paymentResult = paymentInfoService.createPaymentInfo(paymentInfoDTO);
                if (!updateOrderStatus & !addWalletBalance & !paymentResult) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR);
                }
                // 更新活动表
                saveRechargeActivity(productOrder);
                sendSuccessEmail(productOrder, alipayTradeQueryResponse.getTotalAmount());
                log.info("超时订单{},更新成功", orderNo);
            }
        } catch (AlipayApiException e) {
            log.error("订单{} 处理失败", orderNo);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, e.getMessage());
        }

    }

    private void sendSuccessEmail(ApiProductOrder productOrder, String orderTotal) {
        // 发送邮件
        ApiUser user = userService.getById(productOrder.getUserId());
        if (StringUtils.isNotBlank(user.getEmail())) {
            try {
                ApiProductOrder productOrderByOutTradeNo = apiProductOrderServiceImpl.getProductOrderByOutTradeNo(productOrder.getOrderNo());
                new EmailUtil().sendPaySuccessEmail(user.getEmail(), mailSender, emailConfig, productOrderByOutTradeNo.getOrderName(),
                        String.valueOf(orderTotal));
                log.info("发送邮件：{}，成功", user.getEmail());
            } catch (Exception e) {
                log.error("发送邮件：{}，失败：{}", user.getEmail(), e.getMessage());
            }
        }
    }


    public static Map<String, String> toMap(HttpServletRequest request) {
        Map<String, String> params = new HashMap();
        Map<String, String[]> requestParams = request.getParameterMap();
        Iterator<String> iter = requestParams.keySet().iterator();

        while(iter.hasNext()) {
            String name = (String)iter.next();
            String[] values = (String[])requestParams.get(name);
            String valueStr = "";

            for(int i = 0; i < values.length; ++i) {
                valueStr = i == values.length - 1 ? valueStr + values[i] : valueStr + values[i] + ",";
            }

            params.put(name, valueStr);
        }
        return params;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String doPaymentNotify(String notifyData, HttpServletRequest request) {
        Map<String, String> params = toMap(request);
        AliPayAsyncRespDTO aliPayAsyncResponse = JSONUtil.toBean(JSONUtil.toJsonStr(params), AliPayAsyncRespDTO.class);
        String lockName = "notify:AlipayOrder:lock:" + aliPayAsyncResponse.getOutTradeNo();
        return redissonLockUtil.redissonDistributedLocks(lockName, "【支付宝异步回调异常】:", () -> {
            String result;
            try {
                result = checkAlipayOrder(aliPayAsyncResponse, params);
            } catch (AlipayApiException e) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, e.getMessage());
            }
            if (!"success".equals(result)) {
                return result;
            }
            String doAliPayOrderBusinessResult = this.doAliPayOrderBusiness(aliPayAsyncResponse);
            if (StringUtils.isBlank(doAliPayOrderBusinessResult)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }
            return doAliPayOrderBusinessResult;
        });
    }

    private String checkAlipayOrder(AliPayAsyncRespDTO response, Map<String, String> params) throws AlipayApiException {
        String result = "failure";
        boolean verifyResult = AlipaySignature.rsaCheckV1(params, AliPayApiConfigKit.getAliPayApiConfig().getAliPayPublicKey(),
                AliPayApiConfigKit.getAliPayApiConfig().getCharset(),
                AliPayApiConfigKit.getAliPayApiConfig().getSignType());
        if (!verifyResult) {
            return result;
        }
        // 1.验证该通知数据中的 out_trade_no 是否为商家系统中创建的订单号。
        ApiProductOrder productOrder = apiProductOrderServiceImpl.getProductOrderByOutTradeNo(response.getOutTradeNo());
        if (productOrder == null) {
            log.error("订单不存在");
            return result;
        }
        // 2.判断 total_amount 是否确实为该订单的实际金额（即商家订单创建时的金额）。
        int totalAmount = new BigDecimal(response.getTotalAmount()).multiply(new BigDecimal("100")).intValue();
        if (totalAmount != productOrder.getAmount()) {
            log.error("订单金额不一致");
            return result;
        }
        // 3.校验通知中的 seller_id（或者 seller_email) 是否为 out_trade_no 这笔单据的对应的操作方（有的时候，一个商家可能有多个 seller_id/seller_email）。
        String sellerId = aliPayAccountConfig.getSellerId();
        if (!response.getSellerId().equals(sellerId)) {
            log.error("卖家账号校验失败");
            return result;
        }
        // 4.验证 app_id 是否为该商家本身。
        String appId = aliPayAccountConfig.getAppId();
        if (!response.getAppId().equals(appId)) {
            log.error("校验失败");
            return result;
        }
        // 状态 TRADE_SUCCESS 的通知触发条件是商家开通的产品支持退款功能的前提下，买家付款成功。
        String tradeStatus = response.getTradeStatus();
        if (!tradeStatus.equals(TRADE_SUCCESS)) {
            log.error("交易失败");
            return result;
        }
        return "success";
    }

    @SneakyThrows
    protected String doAliPayOrderBusiness(AliPayAsyncRespDTO response) {
        String outTradeNo = response.getOutTradeNo();
        ApiProductOrder productOrder = apiProductOrderServiceImpl.getProductOrderByOutTradeNo(outTradeNo);
        // 处理重复通知
        if (SUCCESS.getValue().equals(productOrder.getStatus())) {
            return "success";
        }
        // 业务代码
        // 更新订单状态
        boolean updateOrderStatus = apiProductOrderServiceImpl.updateOrderStatusByOrderNo(outTradeNo, SUCCESS.getValue());
        // 更新用户积分
        boolean addWalletBalance = userService.addWalletBalance(productOrder.getUserId(), productOrder.getAddPoints(), PointsConstant.USER_RECHARGE);
        // 保存支付记录
        PaymentInfoDTO paymentInfoDTO = new PaymentInfoDTO();
        paymentInfoDTO.setAppid(response.getAppId());
        paymentInfoDTO.setOutTradeNo(response.getOutTradeNo());
        paymentInfoDTO.setTransactionId(response.getTradeNo());
        paymentInfoDTO.setTradeType("电脑网站支付");
        paymentInfoDTO.setTradeState(response.getTradeStatus());
        paymentInfoDTO.setTradeStateDesc("支付成功");
        paymentInfoDTO.setSuccessTime(response.getNotifyTime());
        WxPayOrderQueryV3Result.Payer payer = new WxPayOrderQueryV3Result.Payer();
        payer.setOpenid(response.getBuyerId());
        paymentInfoDTO.setPayer(payer);
        WxPayOrderQueryV3Result.Amount amount = new WxPayOrderQueryV3Result.Amount();
        amount.setTotal(new BigDecimal(response.getTotalAmount()).multiply(new BigDecimal("100")).intValue());
        amount.setPayerTotal(new BigDecimal(response.getReceiptAmount()).multiply(new BigDecimal("100")).intValue());
        amount.setCurrency("CNY");
        amount.setPayerCurrency("CNY");
        paymentInfoDTO.setAmount(amount);
        boolean paymentResult = paymentInfoService.createPaymentInfo(paymentInfoDTO);
        // 更新活动表
        boolean rechargeActivity = saveRechargeActivity(productOrder);
        if (paymentResult && updateOrderStatus && addWalletBalance && rechargeActivity) {
            log.info("【支付回调通知处理成功】");
            // 发送邮件
            sendSuccessEmail(productOrder, response.getTotalAmount());
            return "success";
        }
        throw new BusinessException(ErrorCode.OPERATION_ERROR);
    }


    /**
     * 保存充值活动
     *
     * @param productOrder 产品订单
     * @return boolean
     */
    private boolean saveRechargeActivity(ApiProductOrder productOrder) {
        ApiRechargeActivity rechargeActivity = new ApiRechargeActivity();
        rechargeActivity.setUserId(productOrder.getUserId());
        rechargeActivity.setProductId(productOrder.getProductId());
        rechargeActivity.setOrderNo(productOrder.getOrderNo());
        boolean save = rechargeActivityService.save(rechargeActivity);
        if (!save) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存失败");
        }
        return true;
    }
}




