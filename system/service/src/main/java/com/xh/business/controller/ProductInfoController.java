package com.xh.business.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xh.business.domain.constant.CommonConstant;
import com.xh.business.domain.enums.ProductInfoStatusEnum;
import com.xh.business.domain.model.ApiProductInfo;
import com.xh.business.domain.req.productinfo.ProductInfoAddRequest;
import com.xh.business.domain.req.productinfo.ProductInfoQueryRequest;
import com.xh.business.domain.req.productinfo.ProductInfoSearchTextRequest;
import com.xh.business.domain.req.productinfo.ProductInfoUpdateRequest;
import com.xh.business.domain.resp.user.UserVO;
import com.xh.business.service.ApiProductInfoService;
import com.xh.business.service.ApiUserService;
import com.xh.business.utils.BusinessException;
import com.xh.business.utils.ErrorCode;
import com.xh.common.core.dto.OnlineUserDTO;
import com.xh.common.core.utils.LoginUtil;
import com.xh.common.core.web.DeleteRequest;
import com.xh.common.core.web.IdRequest;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.RestResponse;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 帖子接口
 *
 * @author qimu
 */
@RestController
@RequestMapping("/api/productInfo")
@Slf4j
public class ProductInfoController {

    @Resource
    private ApiProductInfoService productInfoService;
    @Resource
    private ApiUserService userService;

    // region 增删改查

    /**
     * 添加接口信息
     * 创建
     *
     * @param productInfoAddRequest 接口信息添加请求
     * @param request               请求
     * @return {@link RestResponse}<{@link Long}>
     */
    @PostMapping("/add")
    public RestResponse<Long> addProductInfo(@RequestBody ProductInfoAddRequest productInfoAddRequest, HttpServletRequest request) {
        if (productInfoAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ApiProductInfo productInfo = new ApiProductInfo();
        BeanUtils.copyProperties(productInfoAddRequest, productInfo);
        // 校验
        productInfoService.validProductInfo(productInfo, true);
        OnlineUserDTO onlineUserInfo = LoginUtil.getOnlineUserInfo();
        productInfo.setUserId(onlineUserInfo.getUserId());
        boolean result = productInfoService.save(productInfo);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        long newProductInfoId = productInfo.getId();
        return RestResponse.success(newProductInfoId);
    }

    /**
     * 删除接口信息
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return {@link RestResponse}<{@link Boolean}>
     */
    @PostMapping("/delete")
    public RestResponse<Boolean> deleteProductInfo(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (ObjectUtils.anyNull(deleteRequest, deleteRequest.getId()) || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        OnlineUserDTO onlineUserInfo = LoginUtil.getOnlineUserInfo();
        long id = deleteRequest.getId();
        // 判断是否存在
        ApiProductInfo oldProductInfo = productInfoService.getById(id);
        if (oldProductInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!oldProductInfo.getUserId().equals(onlineUserInfo.getUserId()) && Boolean.TRUE.equals(onlineUserInfo.isAdmin())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = productInfoService.removeById(id);
        return RestResponse.success(b);
    }

    /**
     * 更新接口信息
     * 更新
     *
     * @param productInfoUpdateRequest 接口信息更新请求
     * @param request                  请求
     * @return {@link RestResponse}<{@link Boolean}>
     */
    @PostMapping("/update")
    @Transactional(rollbackFor = Exception.class)
    public RestResponse<Boolean> updateProductInfo(@RequestBody ProductInfoUpdateRequest productInfoUpdateRequest,
                                                   HttpServletRequest request) {
        if (ObjectUtils.anyNull(productInfoUpdateRequest, productInfoUpdateRequest.getId()) || productInfoUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ApiProductInfo productInfo = new ApiProductInfo();
        BeanUtils.copyProperties(productInfoUpdateRequest, productInfo);
        // 参数校验
        productInfoService.validProductInfo(productInfo, false);
        OnlineUserDTO onlineUserInfo = LoginUtil.getOnlineUserInfo();
        long id = productInfoUpdateRequest.getId();
        // 判断是否存在
        ApiProductInfo oldProductInfo = productInfoService.getById(id);
        if (oldProductInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可修改
        if (Boolean.FALSE.equals(onlineUserInfo.isAdmin())
                && !oldProductInfo.getUserId().equals(onlineUserInfo.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = productInfoService.updateById(productInfo);
        return RestResponse.success(result);
    }

    /**
     * 通过id获取接口信息
     *
     * @param id id
     * @return {@link RestResponse}<{@link ApiProductInfo}>
     */
    @GetMapping("/get")
    public RestResponse<ApiProductInfo> getProductInfoById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ApiProductInfo productInfo = productInfoService.getById(id);
        return RestResponse.success(productInfo);
    }

    /**
     * 获取列表（仅管理员可使用）
     *
     * @param productInfoQueryRequest 接口信息查询请求
     * @return {@link RestResponse}<{@link List}<{@link ApiProductInfo}>>
     */
    @GetMapping("/list")
    public RestResponse<List<ApiProductInfo>> listProductInfo(ProductInfoQueryRequest productInfoQueryRequest) {
        if (productInfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ApiProductInfo productInfoQuery = new ApiProductInfo();
        BeanUtils.copyProperties(productInfoQueryRequest, productInfoQuery);

        QueryWrapper<ApiProductInfo> queryWrapper = new QueryWrapper<>(productInfoQuery);
        List<ApiProductInfo> productInfoList = productInfoService.list(queryWrapper);
        return RestResponse.success(productInfoList);
    }

    /**
     * 分页获取列表
     *
     * @param pageQuery 接口信息查询请求
     * @param request                 请求
     * @return {@link RestResponse}<{@link Page}<{@link ApiProductInfo}>>
     */
    @PostMapping("/list/page")
    public RestResponse<Page<ApiProductInfo>> listProductInfoByPage(@RequestBody PageQuery<ProductInfoQueryRequest> pageQuery, HttpServletRequest request) {
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
        String description = productInfoQueryRequest.getDescription();
        Integer status = productInfoQueryRequest.getStatus();
        QueryWrapper<ApiProductInfo> queryWrapper = Wrappers.<ApiProductInfo>query()
                .orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        queryWrapper.lambda()
                .like(StringUtils.isNotBlank(name), ApiProductInfo::getName, name)
                .eq(Objects.nonNull(status), ApiProductInfo::getStatus, status)
                .like(StringUtils.isNotBlank(description), ApiProductInfo::getDescription, description);
        return RestResponse.success(productInfoService.page(new Page<>(current, size), queryWrapper));
    }

    /**
     * 分页获取列表
     *
     * @param pageQuery 接口信息查询请求
     * @param request                 请求
     * @return {@link RestResponse}<{@link Page}<{@link ApiProductInfo}>>
     */
    @PostMapping("/get/searchText")
    public RestResponse<Page<ApiProductInfo>> listProductInfoBySearchTextPage(@RequestBody PageQuery<ProductInfoSearchTextRequest> pageQuery, HttpServletRequest request) {
        ProductInfoSearchTextRequest productInfoQueryRequest = pageQuery.getParam();
        if (productInfoQueryRequest == null) {
            productInfoQueryRequest = new ProductInfoSearchTextRequest();
        }
        ApiProductInfo productInfoQuery = new ApiProductInfo();
        BeanUtils.copyProperties(productInfoQueryRequest, productInfoQuery);

        String searchText = productInfoQueryRequest.getSearchText();
        long size = pageQuery.getPageSize();
        long current = pageQuery.getCurrentPage();
        String sortField = pageQuery.getOrderProp();
        String sortOrder = pageQuery.getOrderDirection().name();

        QueryWrapper<ApiProductInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(searchText), "name", searchText)
                .or()
                .like(StringUtils.isNotBlank(searchText), "description", searchText);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        Page<ApiProductInfo> productInfoPage = productInfoService.page(new Page<>(current, size), queryWrapper);
        return RestResponse.success(productInfoPage);
    }

    /**
     * 发布
     *
     * @param idRequest id请求
     * @return {@link RestResponse}<{@link Boolean}>
     */
    @PostMapping("/online")
    public RestResponse<Boolean> onlineProductInfo(@RequestBody IdRequest idRequest) {
        if (ObjectUtils.anyNull(idRequest, idRequest.getId()) || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = idRequest.getId();
        ApiProductInfo productInfo = productInfoService.getById(id);
        if (productInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        productInfo.setStatus(ProductInfoStatusEnum.ONLINE.getValue());
        return RestResponse.success(productInfoService.updateById(productInfo));
    }

    /**
     * 下线
     *
     * @param idRequest id请求
     * @return {@link RestResponse}<{@link Boolean}>
     */
    @PostMapping("/offline")
    public RestResponse<Boolean> offlineProductInfo(@RequestBody IdRequest idRequest) {
        if (ObjectUtils.anyNull(idRequest, idRequest.getId()) || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = idRequest.getId();
        ApiProductInfo productInfo = productInfoService.getById(id);
        if (productInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        productInfo.setStatus(ProductInfoStatusEnum.OFFLINE.getValue());
        return RestResponse.success(productInfoService.updateById(productInfo));
    }
    // endregion
}
