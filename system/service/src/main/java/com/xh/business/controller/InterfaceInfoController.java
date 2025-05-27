package com.xh.business.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xh.business.client.QiApiClient;
import com.xh.business.domain.constant.CommonConstant;
import static com.xh.business.domain.constant.UserConstant.ADMIN_ROLE;
import com.xh.business.domain.enums.InterfaceStatusEnum;
import com.xh.business.domain.model.ApiInterfaceInfo;
import com.xh.business.domain.model.ApiUser;
import com.xh.business.domain.req.interfaceinfo.InterfaceInfoAddRequest;
import com.xh.business.domain.req.interfaceinfo.InterfaceInfoQueryRequest;
import com.xh.business.domain.req.interfaceinfo.InterfaceInfoSearchTextRequest;
import com.xh.business.domain.req.interfaceinfo.InterfaceInfoUpdateAvatarRequest;
import com.xh.business.domain.req.interfaceinfo.InterfaceInfoUpdateRequest;
import com.xh.business.domain.req.interfaceinfo.InvokeRequest;
import com.xh.business.domain.req.interfaceinfo.RequestParamsField;
import com.xh.business.domain.req.interfaceinfo.ResponseParamsField;
import com.xh.business.domain.resp.user.UserVO;
import com.xh.business.model.request.CurrencyRequest;
import com.xh.business.model.response.ResultResponse;
import com.xh.business.service.ApiInterfaceInfoService;
import com.xh.business.service.ApiService;
import com.xh.business.service.ApiUserService;
import com.xh.business.utils.BusinessException;
import com.xh.business.utils.ErrorCode;
import com.xh.common.core.web.DeleteRequest;
import com.xh.common.core.web.IdRequest;
import com.xh.common.core.web.RestResponse;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
@RequestMapping("/api/interfaceInfo")
@Slf4j
public class InterfaceInfoController {
    @Resource
    private ApiInterfaceInfoService interfaceInfoService;
    @Resource
    private ApiUserService userService;
    @Resource
    private ApiService apiService;

    // region 增删改查

    /**
     * 添加接口信息
     * 创建
     *
     * @param interfaceInfoAddRequest 接口信息添加请求
     * @param request                 请求
     * @return {@link RestResponse}<{@link Long}>
     */
    @PostMapping("/add")
    public RestResponse<Long> addInterfaceInfo(@RequestBody InterfaceInfoAddRequest interfaceInfoAddRequest, HttpServletRequest request) {
        if (interfaceInfoAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ApiInterfaceInfo interfaceInfo = new ApiInterfaceInfo();
        if (CollectionUtils.isNotEmpty(interfaceInfoAddRequest.getRequestParams())) {
            List<RequestParamsField> requestParamsFields = interfaceInfoAddRequest.getRequestParams().stream().filter(field -> StringUtils.isNotBlank(field.getFieldName())).collect(Collectors.toList());
            String requestParams = JSON.toJSONString(requestParamsFields);
            interfaceInfo.setRequestParams(requestParams);
        }
        if (CollectionUtils.isNotEmpty(interfaceInfoAddRequest.getResponseParams())) {
            List<ResponseParamsField> responseParamsFields = interfaceInfoAddRequest.getResponseParams().stream().filter(field -> StringUtils.isNotBlank(field.getFieldName())).collect(Collectors.toList());
            String responseParams = JSON.toJSONString(responseParamsFields);
            interfaceInfo.setResponseParams(responseParams);
        }
        BeanUtils.copyProperties(interfaceInfoAddRequest, interfaceInfo);
        // 校验
        interfaceInfoService.validInterfaceInfo(interfaceInfo, true);
        UserVO loginUser = userService.getLoginUser(request);
        interfaceInfo.setUserId(loginUser.getId());
        boolean result = interfaceInfoService.save(interfaceInfo);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        long newInterfaceInfoId = interfaceInfo.getId();
        return RestResponse.success(newInterfaceInfoId);
    }

    /**
     * 删除接口信息
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return {@link RestResponse}<{@link Boolean}>
     */
    @PostMapping("/delete")
    public RestResponse<Boolean> deleteInterfaceInfo(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (ObjectUtils.anyNull(deleteRequest, deleteRequest.getId()) || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserVO user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        ApiInterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!oldInterfaceInfo.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = interfaceInfoService.removeById(id);
        return RestResponse.success(b);
    }

    /**
     * 更新接口头像url
     *
     * @param request                          请求
     * @param interfaceInfoUpdateAvatarRequest 界面信息更新头像请求
     * @return {@link RestResponse}<{@link Boolean}>
     */
    @PostMapping("/updateInterfaceInfoAvatar")
    public RestResponse<Boolean> updateInterfaceInfoAvatarUrl(@RequestBody InterfaceInfoUpdateAvatarRequest interfaceInfoUpdateAvatarRequest,
                                                              HttpServletRequest request) {
        if (ObjectUtils.anyNull(interfaceInfoUpdateAvatarRequest, interfaceInfoUpdateAvatarRequest.getId()) || interfaceInfoUpdateAvatarRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ApiInterfaceInfo interfaceInfo = new ApiInterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoUpdateAvatarRequest, interfaceInfo);
        return RestResponse.success(interfaceInfoService.updateById(interfaceInfo));
    }

    /**
     * 更新接口信息
     *
     * @param interfaceInfoUpdateRequest 接口信息更新请求
     * @param request                    请求
     * @return {@link RestResponse}<{@link Boolean}>
     */
    @PostMapping("/update")
    @Transactional(rollbackFor = Exception.class)
    public RestResponse<Boolean> updateInterfaceInfo(@RequestBody InterfaceInfoUpdateRequest interfaceInfoUpdateRequest,
                                                     HttpServletRequest request) {
        if (ObjectUtils.anyNull(interfaceInfoUpdateRequest, interfaceInfoUpdateRequest.getId()) || interfaceInfoUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ApiInterfaceInfo interfaceInfo = new ApiInterfaceInfo();
        if (CollectionUtils.isNotEmpty(interfaceInfoUpdateRequest.getRequestParams())) {
            List<RequestParamsField> requestParamsFields = interfaceInfoUpdateRequest.getRequestParams().stream().filter(field -> StringUtils.isNotBlank(field.getFieldName())).collect(Collectors.toList());
            String requestParams = JSON.toJSONString(requestParamsFields);
            interfaceInfo.setRequestParams(requestParams);
        }else {
            interfaceInfo.setRequestParams("[]");
        }
        if (CollectionUtils.isNotEmpty(interfaceInfoUpdateRequest.getResponseParams())) {
            List<ResponseParamsField> responseParamsFields = interfaceInfoUpdateRequest.getResponseParams().stream().filter(field -> StringUtils.isNotBlank(field.getFieldName())).collect(Collectors.toList());
            String responseParams = JSON.toJSONString(responseParamsFields);
            interfaceInfo.setResponseParams(responseParams);
        }else {
            interfaceInfo.setResponseParams("[]");
        }
        BeanUtils.copyProperties(interfaceInfoUpdateRequest, interfaceInfo);
        // 参数校验
        interfaceInfoService.validInterfaceInfo(interfaceInfo, false);
        UserVO user = userService.getLoginUser(request);
        long id = interfaceInfoUpdateRequest.getId();
        // 判断是否存在
        ApiInterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可修改
        if (!userService.isAdmin(request) && !oldInterfaceInfo.getUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = interfaceInfoService.updateById(interfaceInfo);
        return RestResponse.success(result);
    }

    /**
     * 通过id获取接口信息
     *
     * @param id id
     * @return {@link RestResponse}<{@link ApiInterfaceInfo}>
     */
    @GetMapping("/get")
    public RestResponse<ApiInterfaceInfo> getInterfaceInfoById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ApiInterfaceInfo interfaceInfo = interfaceInfoService.getById(id);
        return RestResponse.success(interfaceInfo);
    }

    /**
     * 获取列表（仅管理员可使用）
     *
     * @param interfaceInfoQueryRequest 接口信息查询请求
     * @return {@link RestResponse}<{@link List}<{@link ApiInterfaceInfo}>>
     */
    @GetMapping("/list")
    public RestResponse<List<ApiInterfaceInfo>> listInterfaceInfo(InterfaceInfoQueryRequest interfaceInfoQueryRequest) {
        if (interfaceInfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ApiInterfaceInfo interfaceInfoQuery = new ApiInterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoQueryRequest, interfaceInfoQuery);

        QueryWrapper<ApiInterfaceInfo> queryWrapper = new QueryWrapper<>(interfaceInfoQuery);
        List<ApiInterfaceInfo> interfaceInfoList = interfaceInfoService.list(queryWrapper);
        return RestResponse.success(interfaceInfoList);
    }

    /**
     * 分页获取列表
     * @param interfaceInfoQueryRequest 接口信息查询请求
     * @param request                   请求
     * @return {@link RestResponse}<{@link Page}<{@link ApiInterfaceInfo}>>
     */
    @GetMapping("/list/page")
    public RestResponse<Page<ApiInterfaceInfo>> listInterfaceInfoByPage(InterfaceInfoQueryRequest interfaceInfoQueryRequest, HttpServletRequest request) {
        if (interfaceInfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        ApiInterfaceInfo interfaceInfoQuery = new ApiInterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoQueryRequest, interfaceInfoQuery);
        long size = interfaceInfoQueryRequest.getPageSize();
        String sortField = interfaceInfoQueryRequest.getOrderProp();
        String sortOrder = interfaceInfoQueryRequest.getOrderDirection().name();
        String url = interfaceInfoQueryRequest.getUrl();

        String name = interfaceInfoQueryRequest.getName();
        long current = interfaceInfoQueryRequest.getCurrentPage();
        String method = interfaceInfoQueryRequest.getMethod();
        String description = interfaceInfoQueryRequest.getDescription();
        Integer status = interfaceInfoQueryRequest.getStatus();
        Integer reduceScore = interfaceInfoQueryRequest.getReduceScore();
        String returnFormat = interfaceInfoQueryRequest.getReturnFormat();
        // 限制爬虫
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<ApiInterfaceInfo> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(description)) {
            queryWrapper.and(qw -> qw.like("name", name).or()
                    .like("description", description));
        }
        queryWrapper
                .like(StringUtils.isNotBlank(url), "url", url)
                .like(StringUtils.isNotBlank(returnFormat), "returnFormat", returnFormat)
                .eq(StringUtils.isNotBlank(method), "method", method)
                .eq(ObjectUtils.isNotEmpty(status), "status", status)
                .eq(ObjectUtils.isNotEmpty(reduceScore), "reduceScore", reduceScore);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        Page<ApiInterfaceInfo> interfaceInfoPage = interfaceInfoService.page(new Page<>(current, size), queryWrapper);
        ApiUser user = userService.isTourist(request);
        // 不是管理员只能查看已经上线的
        if (user == null || !user.getUserRole().equals(ADMIN_ROLE)) {
            List<ApiInterfaceInfo> interfaceInfoList = interfaceInfoPage.getRecords().stream()
                    .filter(interfaceInfo -> interfaceInfo.getStatus().equals(InterfaceStatusEnum.ONLINE.getValue())).collect(Collectors.toList());
            interfaceInfoPage.setRecords(interfaceInfoList);
        }
        return RestResponse.success(interfaceInfoPage);
    }

    /**
     * 按搜索文本页查询数据
     *
     * @param interfaceInfoQueryRequest 接口信息查询请求
     * @param request                   请求
     * @return {@link RestResponse}<{@link Page}<{@link ApiInterfaceInfo}>>
     */
    @GetMapping("/get/searchText")
    public RestResponse<Page<ApiInterfaceInfo>> listInterfaceInfoBySearchTextPage(InterfaceInfoSearchTextRequest interfaceInfoQueryRequest, HttpServletRequest request) {
        if (interfaceInfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ApiInterfaceInfo interfaceInfoQuery = new ApiInterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoQueryRequest, interfaceInfoQuery);

        String searchText = interfaceInfoQueryRequest.getSearchText();
        long size = interfaceInfoQueryRequest.getPageSize();
        long current = interfaceInfoQueryRequest.getCurrentPage();
        String sortField = interfaceInfoQueryRequest.getOrderProp();
        String sortOrder = interfaceInfoQueryRequest.getOrderDirection().name();

        QueryWrapper<ApiInterfaceInfo> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like(StringUtils.isNotBlank(searchText), "name", searchText)
                    .or()
                    .like(StringUtils.isNotBlank(searchText), "description", searchText));
        }
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        Page<ApiInterfaceInfo> interfaceInfoPage = interfaceInfoService.page(new Page<>(current, size), queryWrapper);
        // 不是管理员只能查看已经上线的
        if (!userService.isAdmin(request)) {
            List<ApiInterfaceInfo> interfaceInfoList = interfaceInfoPage.getRecords().stream()
                    .filter(interfaceInfo -> interfaceInfo.getStatus().equals(InterfaceStatusEnum.ONLINE.getValue())).collect(Collectors.toList());
            interfaceInfoPage.setRecords(interfaceInfoList);
        }
        return RestResponse.success(interfaceInfoPage);
    }

    /**
     * 发布
     *
     * @param idRequest id请求
     * @return {@link RestResponse}<{@link Boolean}>
     */
    @PostMapping("/online")
    public RestResponse<Boolean> onlineInterfaceInfo(@RequestBody IdRequest idRequest, HttpServletRequest request) {
        if (ObjectUtils.anyNull(idRequest, idRequest.getId()) || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = idRequest.getId();
        ApiInterfaceInfo interfaceInfo = interfaceInfoService.getById(id);
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        interfaceInfo.setStatus(InterfaceStatusEnum.ONLINE.getValue());
        return RestResponse.success(interfaceInfoService.updateById(interfaceInfo));
    }

    /**
     * 下线
     *
     * @param idRequest id请求
     * @param request   请求
     * @return {@link RestResponse}<{@link Boolean}>
     */
    @PostMapping("/offline")
    public RestResponse<Boolean> offlineInterfaceInfo(@RequestBody IdRequest idRequest, HttpServletRequest request) {
        if (ObjectUtils.anyNull(idRequest, idRequest.getId()) || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = idRequest.getId();
        ApiInterfaceInfo interfaceInfo = interfaceInfoService.getById(id);
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        interfaceInfo.setStatus(InterfaceStatusEnum.OFFLINE.getValue());
        return RestResponse.success(interfaceInfoService.updateById(interfaceInfo));
    }

    // endregion

    /**
     * 调用接口
     *
     * @param invokeRequest id请求
     * @param request       请求
     * @return {@link RestResponse}<{@link Object}>
     */
    @PostMapping("/invoke")
    @Transactional(rollbackFor = Exception.class)
    public RestResponse<Object> invokeInterface(@RequestBody InvokeRequest invokeRequest, HttpServletRequest request) {
        if (ObjectUtils.anyNull(invokeRequest, invokeRequest.getId()) || invokeRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = invokeRequest.getId();
        ApiInterfaceInfo interfaceInfo = interfaceInfoService.getById(id);
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (interfaceInfo.getStatus() != InterfaceStatusEnum.ONLINE.getValue()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口未开启");
        }
        // 构建请求参数
        List<InvokeRequest.Field> fieldList = invokeRequest.getRequestParams();
        String requestParams = "{}";
        if (fieldList != null && fieldList.size() > 0) {
            JSONObject jsonObject = new JSONObject();
            for (InvokeRequest.Field field : fieldList) {
                jsonObject.put(field.getFieldName(), field.getValue());
            }
            requestParams = JSON.toJSONString(jsonObject);
        }
        Map<String, Object> params = JSON.parseObject(requestParams, new TypeReference<Map<String, Object>>() {
        });
        UserVO loginUser = userService.getLoginUser(request);
        String accessKey = loginUser.getAccessKey();
        String secretKey = loginUser.getSecretKey();
        try {
            QiApiClient qiApiClient = new QiApiClient(accessKey, secretKey);
            CurrencyRequest currencyRequest = new CurrencyRequest();
            currencyRequest.setMethod(interfaceInfo.getMethod());
            currencyRequest.setPath(interfaceInfo.getUrl());
            currencyRequest.setRequestParams(params);
            ResultResponse response = apiService.request(qiApiClient, currencyRequest);
            return RestResponse.success(response.getData());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }
}
