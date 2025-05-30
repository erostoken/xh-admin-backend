package com.xh.business.controller.inner;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xh.business.ddd.order.OrderService;
import static com.xh.business.domain.constant.PayConstant.QUERY_ORDER_STATUS;
import static com.xh.business.domain.enums.ImageStatusEnum.SUCCESS;
import com.xh.business.domain.enums.PaymentStatusEnum;
import com.xh.business.domain.model.ApiProductInfo;
import com.xh.business.domain.model.ApiProductOrder;
import com.xh.business.domain.req.productorder.ProductOrderCreateRequest;
import com.xh.business.domain.req.productorder.ProductOrderQueryRequest;
import com.xh.business.domain.resp.order.OrderVO;
import com.xh.business.domain.resp.order.ProductOrderVO;
import com.xh.business.domain.resp.user.UserVO;
import com.xh.business.service.ApiProductOrderService;
import com.xh.business.service.ApiUserService;
import com.xh.business.utils.BusinessException;
import com.xh.business.utils.ErrorCode;
import com.xh.common.core.web.RestResponse;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @Author: QiMu
 * @Date: 2023年08月23日 00:13
 * @Version: 1.0
 * @Description:
 */
@RestController
@Slf4j
@RequestMapping("/api/order")
public class OrderController {
    @Resource
    private ApiUserService userService;
    @Resource
    private ApiProductOrderService productOrderService;
    @Resource
    private OrderService orderService;
    @Resource
    private RedisTemplate<String, Boolean> redisTemplate;

    // region 增删改查

    /**
     * 取消订单订单
     *
     * @return {@link RestResponse}<{@link Boolean}>
     */
    @PostMapping("/closed")
    public RestResponse<Boolean> closedProductOrder(String orderNo) {
        if (StringUtils.isBlank(orderNo)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        ApiProductOrder productOrder = productOrderService.getProductOrderByOutTradeNo(orderNo);
        if (productOrder == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        boolean closedResult = productOrderService.updateOrderStatusByOrderNo(orderNo, PaymentStatusEnum.CLOSED.getValue());
        return RestResponse.success(closedResult);
    }

    @PostMapping("/delete")
    public RestResponse<Boolean> deleteProductOrder(String id, HttpServletRequest request) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserVO loginUser = userService.getLoginUser(request);
        // 校验数据是否存在
        ApiProductOrder productOrder = productOrderService.getById(id);
        if (productOrder == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!productOrder.getUserId().equals(loginUser.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return RestResponse.success(productOrderService.removeById(id));
    }

    /**
     * 按id获取产品订单
     *
     * @param id id
     * @return {@link RestResponse}<{@link ProductOrderVO}>
     */
    @GetMapping("/get")
    public RestResponse<ProductOrderVO> getProductOrderById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ApiProductOrder productOrder = productOrderService.getById(id);
        if (productOrder == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        ProductOrderVO ProductOrderVO = formatProductOrderVO(productOrder);
        return RestResponse.success(ProductOrderVO);
    }

    /**
     * 分页获取列表
     *
     * @param productOrderQueryRequest 接口信息查询请求
     * @param request                  请求
     * @return {@link RestResponse}<{@link Page}<{@link OrderVO}>>
     */
    @GetMapping("/list/page")
    public RestResponse<OrderVO> listProductOrderByPage(ProductOrderQueryRequest productOrderQueryRequest, HttpServletRequest request) {
        if (productOrderQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ApiProductOrder productOrder = new ApiProductOrder();
        BeanUtils.copyProperties(productOrderQueryRequest, productOrder);
        long size = productOrderQueryRequest.getPageSize();
        String orderName = productOrderQueryRequest.getOrderName();
        String orderNo = productOrderQueryRequest.getOrderNo();
        Integer total = productOrderQueryRequest.getTotal();
        String status = productOrderQueryRequest.getStatus();
        String productInfo = productOrderQueryRequest.getProductInfo();
        String payType = productOrderQueryRequest.getPayType();
        Integer addPoints = productOrderQueryRequest.getAddPoints();
        long current = productOrderQueryRequest.getCurrentPage();

        // 限制爬虫
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserVO loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        QueryWrapper<ApiProductOrder> queryWrapper = new QueryWrapper<>();

        queryWrapper.like(StringUtils.isNotBlank(orderName), "orderName", orderName)
                .like(StringUtils.isNotBlank(productInfo), "productInfo", productInfo)
                .eq("userId", userId)
                .eq(StringUtils.isNotBlank(orderNo), "orderNo", orderNo)
                .eq(StringUtils.isNotBlank(status), "status", status)
                .eq(StringUtils.isNotBlank(payType), "payType", payType)
                .eq(ObjectUtils.isNotEmpty(addPoints), "addPoints", addPoints)
                .eq(ObjectUtils.isNotEmpty(total), "total", total);
        // 未支付的订单前置
        queryWrapper.last("ORDER BY CASE WHEN status = 'NOTPAY' THEN 0 ELSE 1 END, status");
        Page<ApiProductOrder> productOrderPage = productOrderService.page(new Page<>(current, size), queryWrapper);
        OrderVO orderVo = new OrderVO();
        BeanUtils.copyProperties(productOrderPage, orderVo);
        // 处理订单信息,
        List<ProductOrderVO> productOrders = productOrderPage.getRecords().stream().map(this::formatProductOrderVO).collect(Collectors.toList());
        orderVo.setRecords(productOrders);
        return RestResponse.success(orderVo);
    }
    // endregion

    /**
     * 创建订单
     *
     * @param request          要求
     * @param productOrderCreateRequest 付款创建请求
     * @return {@link RestResponse}<{@link ProductOrderVO}>
     */
    @PostMapping("/create")
    public RestResponse<ProductOrderVO> createOrder(@RequestBody ProductOrderCreateRequest productOrderCreateRequest, HttpServletRequest request) {
        if (ObjectUtils.anyNull(productOrderCreateRequest) || StringUtils.isBlank(productOrderCreateRequest.getProductId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long productId = Long.valueOf(productOrderCreateRequest.getProductId());
        String payType = productOrderCreateRequest.getPayType();
        if (StringUtils.isBlank(payType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "暂无该支付方式");
        }
        UserVO loginUser = userService.getLoginUser(request);
        ProductOrderVO productOrderVO = orderService.createOrderByPayType(productId, payType, loginUser);
        if (productOrderVO == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "订单创建失败，请稍后再试");
        }
        return RestResponse.success(productOrderVO);
    }


    /**
     * 查询订单状态
     *
     * @param productOrderQueryRequest 接口订单查询请求
     * @return {@link RestResponse}<{@link Boolean}>
     */
    @PostMapping("/query/status")
    public RestResponse<Boolean> queryOrderStatus(@RequestBody ProductOrderQueryRequest productOrderQueryRequest) {
        if (ObjectUtils.isEmpty(productOrderQueryRequest) || StringUtils.isBlank(productOrderQueryRequest.getOrderNo())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String orderNo = productOrderQueryRequest.getOrderNo();
        Boolean data = redisTemplate.opsForValue().get(QUERY_ORDER_STATUS + orderNo);
        if (Boolean.FALSE.equals(data)) {
            return RestResponse.success(data);
        }
        ApiProductOrder productOrder = productOrderService.getProductOrderByOutTradeNo(orderNo);
        if (SUCCESS.getValue().equals(productOrder.getStatus())) {
            return RestResponse.success(true);
        }
        redisTemplate.opsForValue().set(QUERY_ORDER_STATUS + orderNo, false, 5, TimeUnit.MINUTES);
        return RestResponse.success(false);
    }

    /**
     * 解析订单通知结果
     * 通知频率为15s/15s/30s/3m/10m/20m/30m/30m/30m/60m/3h/3h/3h/6h/6h - 总计 24h4m
     *
     * @param notifyData 通知数据
     * @param request    请求
     * @return {@link String}
     */

    @PostMapping("/notify/order")
    public String parseOrderNotifyResult(@RequestBody String notifyData, HttpServletRequest request) {
        return orderService.doOrderNotify(notifyData, request);
    }

    private ProductOrderVO formatProductOrderVO(ApiProductOrder productOrder) {
        ProductOrderVO ProductOrderVO = new ProductOrderVO();
        BeanUtils.copyProperties(productOrder, ProductOrderVO);
        ApiProductInfo prodInfo = JSONUtil.toBean(productOrder.getProductInfo(), ApiProductInfo.class);
        ProductOrderVO.setDescription(prodInfo.getDescription());
        ProductOrderVO.setProductType(prodInfo.getProductType());
        String voTotal = String.valueOf(prodInfo.getAmount());
        BigDecimal total = new BigDecimal(voTotal).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        ProductOrderVO.setAmount(total.toString());
        return ProductOrderVO;
    }
}
