package com.xh.business.controller.ex;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xh.business.domain.constant.CommonConstant;
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
@RequestMapping("/api/ex/product")
@Slf4j
@Tag(name = "外部API产品")
public class ExProductController {

    @Resource
    private ApiProductInfoService apiProductInfoService;

    /**
     * 分页获取列表
     *
     * @param pageQuery 接口信息查询请求
     * @return {@link RestResponse}<{@link Page}<{@link ApiProductInfo}>>
     */
    @Operation(description = "分页获取列表")
    @PostMapping("/list/page")
    public RestResponse<Page<ApiProductInfo>> listProductInfoByPage(@RequestBody PageQuery<ProductInfoQueryRequest> pageQuery) {
        ProductInfoQueryRequest productInfoQueryRequest = pageQuery.getParam();
        if (productInfoQueryRequest == null) {
            productInfoQueryRequest = new ProductInfoQueryRequest();
        }

        long current = pageQuery.getCurrentPage();
        long size = pageQuery.getPageSize();
        // 根据金额升序排列
        String sortField = pageQuery.getOrderProp();
        String sortOrder = pageQuery.getOrderDirection().name();
        if(Objects.isNull(pageQuery.getOrderProp())) {
            sortField = "amount";
            sortOrder = CommonConstant.SORT_ORDER_ASC;
        }

        ApiProductInfo productInfoQuery = new ApiProductInfo();
        BeanUtils.copyProperties(productInfoQueryRequest, productInfoQuery);
        String name = productInfoQueryRequest.getName();
        QueryWrapper<ApiProductInfo> queryWrapper = Wrappers.<ApiProductInfo>query()
                .orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        queryWrapper.lambda()
                .like(StringUtils.isNotBlank(name), ApiProductInfo::getName, name)
                .eq(ApiProductInfo::getStatus, ProductInfoStatusEnum.ONLINE.getValue());
        return RestResponse.success(apiProductInfoService.page(new Page<>(current, size), queryWrapper));
    }

}
