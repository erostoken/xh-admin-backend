package com.xh.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xh.business.domain.model.ApiInterfaceInfo;
import com.xh.business.mapper.ApiInterfaceInfoMapper;
import com.xh.business.service.ApiInterfaceInfoService;
import com.xh.business.utils.BusinessException;
import com.xh.business.utils.ErrorCode;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
* @author apple
* @description 针对表【api_interface_info(接口信息)】的数据库操作Service实现
* @createDate 2025-05-26 16:55:21
*/
@Service
public class ApiInterfaceInfoServiceImpl extends ServiceImpl<ApiInterfaceInfoMapper, ApiInterfaceInfo>
    implements ApiInterfaceInfoService{

    @Override
    public void validInterfaceInfo(ApiInterfaceInfo interfaceInfo, boolean add) {
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String name = interfaceInfo.getName();
        String url = interfaceInfo.getUrl();
        Long userId = interfaceInfo.getUserId();
        String method = interfaceInfo.getMethod();

        String requestParams = interfaceInfo.getRequestParams();
        String description = interfaceInfo.getDescription();
        String requestExample = interfaceInfo.getRequestExample();
        String requestHeader = interfaceInfo.getRequestHeader();
        String responseHeader = interfaceInfo.getResponseHeader();
        Integer status = interfaceInfo.getStatus();
        Long reduceScore = interfaceInfo.getReduceScore();
        // 创建时，所有参数必须非空
        if (add) {
            if (StringUtils.isAnyBlank(name, url, method)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
        }
        if (StringUtils.isNotBlank(method)) {
            interfaceInfo.setMethod(method.trim().toUpperCase());
        }
        if (StringUtils.isNotBlank(url)) {
            interfaceInfo.setUrl(url.trim());
        }
        if (ObjectUtils.isNotEmpty(reduceScore) && reduceScore < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "扣除积分个数不能为负数");
        }
        if (StringUtils.isNotBlank(name) && name.length() > 60) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口名称过长");
        }
        if (StringUtils.isNotBlank(description) && description.length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口描述过长");
        }
    }

    @Override
    public boolean updateTotalInvokes(long interfaceId) {
        LambdaUpdateWrapper<ApiInterfaceInfo> invokeLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        invokeLambdaUpdateWrapper.eq(ApiInterfaceInfo::getId, interfaceId);
        invokeLambdaUpdateWrapper.setSql("totalInvokes = totalInvokes + 1");
        return this.update(invokeLambdaUpdateWrapper);
    }

}




