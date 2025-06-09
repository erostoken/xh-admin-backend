package com.xh.business.ddd.user;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xh.business.ddd.PageUtils;
import com.xh.business.domain.constant.CommonConstant;
import com.xh.business.domain.enums.InterfaceStatusEnum;
import com.xh.business.domain.model.ApiInterfaceInfo;
import com.xh.business.domain.model.ApiUserInterfaceApply;
import com.xh.business.domain.req.interfaceinfo.InterfaceInfoQueryRequest;
import com.xh.business.domain.resp.api.InterfaceInfoVO;
import com.xh.business.domain.resp.user.UserVO;
import com.xh.business.service.ApiInterfaceInfoService;
import com.xh.business.service.ApiUserInterfaceApplyService;
import com.xh.business.service.ApiUserInterfaceInvokeService;
import com.xh.business.service.ApiUserService;
import com.xh.business.utils.BusinessException;
import com.xh.business.utils.ErrorCode;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.RestResponse;
import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * @author xuwentao
 * @version 1.0
 * @date 2025/5/30
 * @description 外部API接口聚合层
 */
@Slf4j
@Component
public class ExApiInterfaceAggregate {

    @Resource
    private ApiInterfaceInfoService interfaceInfoService;
    @Resource
    private ApiUserInterfaceInvokeService userInterfaceInvokeService;
    @Resource
    private ApiUserInterfaceApplyService userInterfaceApplyService;
    @Resource
    private ApiUserService apiUserService;

    /**
     * 接口分类列表
     * @return {@link List}<{@link Map}>
     */
    public List<Map<String, Object>> categoryList() {
        return Collections.singletonList(Map.of("key", "基本核验", "label", "基本核验"));
    }

    /**
     * 分页获取列表
     * @param pageQuery 接口信息查询请求
     * @return {@link Page}<{@link ApiInterfaceInfo}>
     */
    public Page<InterfaceInfoVO> listInterfaceInfoByPage(PageQuery<InterfaceInfoQueryRequest> pageQuery) {
        InterfaceInfoQueryRequest interfaceInfoQueryRequest = pageQuery.getParam();
        if (interfaceInfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long current = pageQuery.getCurrentPage();
        long size = pageQuery.getPageSize();
        String sortField = StringUtils.isBlank(pageQuery.getOrderProp()) ? "id" : pageQuery.getOrderProp();
        String sortOrder = pageQuery.getOrderDirection().name();

        String keyword = interfaceInfoQueryRequest.getKeyword();

        // 请求信息封装
        QueryWrapper<ApiInterfaceInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .like(StringUtils.isNotBlank(keyword), ApiInterfaceInfo::getUrl, keyword)
                .or().like(StringUtils.isNotBlank(keyword), ApiInterfaceInfo::getName, keyword)
                .eq(ApiInterfaceInfo::getStatus, InterfaceStatusEnum.ONLINE.getValue());
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        Page<ApiInterfaceInfo> page = interfaceInfoService.page(new Page<>(current, size), queryWrapper);
        if (CollUtil.isEmpty(page.getRecords())) {
            return PageUtils.toPage(page, Collections.emptyList());
        }

        Page<InterfaceInfoVO> voPage = PageUtils.toPage(page, (item) -> {
            return InterfaceInfoVO.builder()
                    .id(item.getId())
                    .name(item.getName())
                    .avatarUrl(item.getAvatarUrl())
                    .url(item.getUrl())
                    .method(item.getMethod())
                    .requestParams(item.getRequestParams())
                    .responseParams(item.getResponseParams())
                    .reduceScore(item.getReduceScore())
                    .returnFormat(item.getReturnFormat())
                    .description(item.getDescription())
                    .status(item.getStatus())
                    .build();
        });
        UserVO user = apiUserService.getUser();
        List<InterfaceInfoVO> records = voPage.getRecords();
        Set<Long> interfaceIdList = records.stream().map(InterfaceInfoVO::getId).collect(Collectors.toSet());
        List<ApiUserInterfaceApply> userInterfaceApplyList = userInterfaceApplyService.lambdaQuery()
                .in(ApiUserInterfaceApply::getInterfaceId, interfaceIdList)
                .eq(ApiUserInterfaceApply::getApplyId, user.getId())
                .list();
        Map<Long, Integer> interfaceApplyMap = userInterfaceApplyList.stream().collect(Collectors.toMap(ApiUserInterfaceApply::getInterfaceId, ApiUserInterfaceApply::getStatus));
        for (InterfaceInfoVO record : records) {
            record.setApplyStatus(interfaceApplyMap.getOrDefault(record.getId(), null));
        }
        return voPage;
    }

    /**
     * 通过id获取接口信息
     *
     * @param id id
     * @return {@link ApiInterfaceInfo}
     */
    public InterfaceInfoVO getById(long id) {
        UserVO user = apiUserService.getUser();

        Optional<ApiInterfaceInfo> apiInterfaceInfoOpt = interfaceInfoService.lambdaQuery().eq(ApiInterfaceInfo::getId, id)
                .eq(ApiInterfaceInfo::getStatus, InterfaceStatusEnum.ONLINE.getValue())
                .oneOpt();
        ApiInterfaceInfo apiInterfaceInfo = apiInterfaceInfoOpt.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR));

        Optional<ApiUserInterfaceApply> userInterfaceApplyOpt = userInterfaceApplyService.lambdaQuery()
                .eq(ApiUserInterfaceApply::getInterfaceId, apiInterfaceInfo.getId())
                .eq(ApiUserInterfaceApply::getApplyId, user.getId())
                .oneOpt();
        InterfaceInfoVO interfaceInfoVO = new InterfaceInfoVO();
        BeanUtils.copyProperties(apiInterfaceInfo, interfaceInfoVO);
        interfaceInfoVO.setApplyStatus(userInterfaceApplyOpt.map(ApiUserInterfaceApply::getStatus).orElse(null));
        return interfaceInfoVO;
    }

    /**
     * 申请接口
     *
     * @param interfaceId 接口ID
     * @return {@link RestResponse}<{@link InterfaceInfoVO}>
     */
    public void applyInterfaceInfo(Long interfaceId) {
        UserVO user = apiUserService.getUser();
        Long userId = user.getId();

        Optional<ApiUserInterfaceApply> userInterfaceApplyOpt = userInterfaceApplyService.lambdaQuery()
                .eq(ApiUserInterfaceApply::getInterfaceId, interfaceId)
                .eq(ApiUserInterfaceApply::getApplyId, userId)
                .oneOpt();
        ApiUserInterfaceApply apiUserInterfaceApply;
        if (userInterfaceApplyOpt.isPresent()) {
            apiUserInterfaceApply = userInterfaceApplyOpt.get();
            if(0 == apiUserInterfaceApply.getStatus()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "该接口正在申请中, 请稍后");
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该接口申请已通过, 请勿重复申请");
        }

        apiUserInterfaceApply = new ApiUserInterfaceApply();
        apiUserInterfaceApply.setApplyId(userId);
        apiUserInterfaceApply.setInterfaceId(interfaceId);
        apiUserInterfaceApply.setStatus(0);
        apiUserInterfaceApply.setApplyTime(new Date());
        userInterfaceApplyService.save(apiUserInterfaceApply);
    }

    /**
     * 重新申请接口
     *
     * @param interfaceId 接口ID
     */
    public void reapplyInterfaceInfo(Long interfaceId) {
        UserVO user = apiUserService.getUser();
        Long userId = user.getId();

        Optional<ApiUserInterfaceApply> userInterfaceApplyOpt = userInterfaceApplyService.lambdaQuery()
                .eq(ApiUserInterfaceApply::getInterfaceId, interfaceId)
                .eq(ApiUserInterfaceApply::getApplyId, userId)
                .oneOpt();

        ApiUserInterfaceApply apiUserInterfaceApply = userInterfaceApplyOpt
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到申请记录"));

        if (apiUserInterfaceApply.getStatus() == 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该接口正在申请中");
        }

        // 更新状态为待审核，并更新申请时间
        apiUserInterfaceApply.setStatus(0);
        apiUserInterfaceApply.setApplyTime(new Date());
        userInterfaceApplyService.updateById(apiUserInterfaceApply);
    }

    /**
     * 申请获取列表
     * @param pageQuery 接口信息查询请求
     * @return {@link RestResponse}<{@link Page}<{@link InterfaceInfoVO}>>
     */
    public Page<InterfaceInfoVO> applyInterfaceInfoByPage(PageQuery<InterfaceInfoQueryRequest> pageQuery) {
        InterfaceInfoQueryRequest interfaceInfoQueryRequest = pageQuery.getParam();
        if (interfaceInfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long current = pageQuery.getCurrentPage();
        long size = pageQuery.getPageSize();
        String sortField = StringUtils.isBlank(pageQuery.getOrderProp()) ? "id" : pageQuery.getOrderProp();
        String sortOrder = pageQuery.getOrderDirection().name();

        UserVO user = apiUserService.getUser();
        interfaceInfoQueryRequest.setApplyUserId(user.getId());

        // 请求信息封装
        Page<InterfaceInfoVO> voPage = new Page<>(current, size);
        voPage.setOrders(Collections.singletonList(sortOrder.equals(CommonConstant.SORT_ORDER_ASC) ? OrderItem.asc(sortField) : OrderItem.desc(sortField)));
        voPage.setRecords(interfaceInfoService.applyInterfaceList(voPage, interfaceInfoQueryRequest));
        if (CollUtil.isEmpty(voPage.getRecords())) {
            return PageUtils.toPage(voPage, Collections.emptyList());
        }

        List<InterfaceInfoVO> records = voPage.getRecords();
        Set<Long> interfaceIdList = records.stream().map(InterfaceInfoVO::getId).collect(Collectors.toSet());
        List<ApiUserInterfaceApply> userInterfaceApplyList = userInterfaceApplyService.lambdaQuery()
                .in(ApiUserInterfaceApply::getInterfaceId, interfaceIdList)
                .eq(ApiUserInterfaceApply::getApplyId, user.getId())
                .list();
        Map<Long, Integer> interfaceApplyMap = userInterfaceApplyList.stream().collect(Collectors.toMap(ApiUserInterfaceApply::getInterfaceId, ApiUserInterfaceApply::getStatus));
        for (InterfaceInfoVO record : records) {
            record.setApplyStatus(interfaceApplyMap.getOrDefault(record.getId(), null));
        }
        return voPage;
    }
}
