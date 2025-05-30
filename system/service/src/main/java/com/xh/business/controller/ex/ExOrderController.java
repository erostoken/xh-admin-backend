package com.xh.business.controller.ex;

import cn.dev33.satoken.annotation.SaIgnore;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xh.business.ddd.user.ExApiOrderAggregate;
import com.xh.business.domain.constant.CommonConstant;
import com.xh.business.domain.enums.PayTypeStatusEnum;
import com.xh.business.domain.enums.ProductInfoStatusEnum;
import com.xh.business.domain.model.ApiProductInfo;
import com.xh.business.domain.req.productinfo.ProductInfoQueryRequest;
import com.xh.business.domain.req.productorder.ProductOrderCreateRequest;
import com.xh.business.domain.resp.order.ProductOrderVO;
import com.xh.business.domain.resp.user.UserVO;
import com.xh.business.service.ApiProductInfoService;
import com.xh.business.utils.BusinessException;
import com.xh.business.utils.ErrorCode;
import com.xh.common.core.configuration.ExUser;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author xuwentao
 * @version 1.0
 * @date 2025/5/30
 * @description 备注信息
 */
@ExUser
@RestController
@RequestMapping("/api/ex/order")
@Slf4j
@Tag(name = "外部API订单")
public class ExOrderController {

    @Resource
    private ExApiOrderAggregate exApiOrderAggregate;

    /**
     * 获取支付方式
     *
     * @return {@link RestResponse}<{@link List}<{@link String}>>
     */
    @SaIgnore
    @Operation(description = "获取支付方式")
    @PostMapping("/get/payType")
    public RestResponse<List<String>> getPayType() {
        return RestResponse.success(PayTypeStatusEnum.getValues());
    }

    /**
     * 创建订单
     *
     * @param request          要求
     * @param productOrderCreateRequest 付款创建请求
     * @return {@link RestResponse}<{@link ProductOrderVO}>
     */
    @PostMapping("/create/product")
    public RestResponse<ProductOrderVO> createProductOrder(@RequestBody ProductOrderCreateRequest productOrderCreateRequest, HttpServletRequest request) {
        if (ObjectUtils.anyNull(productOrderCreateRequest) || StringUtils.isBlank(productOrderCreateRequest.getProductId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long productId = Long.valueOf(productOrderCreateRequest.getProductId());
        String payType = productOrderCreateRequest.getPayType();
        if (StringUtils.isBlank(payType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "暂无该支付方式");
        }
        ProductOrderVO productOrderVO = exApiOrderAggregate.createProductByPayType(productId, payType);
        if (productOrderVO == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "订单创建失败，请稍后再试");
        }
        return RestResponse.success(productOrderVO);
    }

}
