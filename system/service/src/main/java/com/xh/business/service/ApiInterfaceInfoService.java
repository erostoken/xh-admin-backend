package com.xh.business.service;

import com.xh.business.domain.model.ApiInterfaceInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author apple
* @description 针对表【api_interface_info(接口信息)】的数据库操作Service
* @createDate 2025-05-26 16:55:21
*/
public interface ApiInterfaceInfoService extends IService<ApiInterfaceInfo> {

    /**
     * 校验
     *
     * @param add           是否为创建校验
     * @param interfaceInfo 接口信息
     */
    void validInterfaceInfo(ApiInterfaceInfo interfaceInfo, boolean add);

    /**
     * 更新总调用数
     *
     * @param interfaceId 接口id
     * @return boolean
     */
    boolean updateTotalInvokes(long interfaceId);

}
