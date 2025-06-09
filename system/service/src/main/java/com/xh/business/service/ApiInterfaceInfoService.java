package com.xh.business.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xh.business.domain.model.ApiInterfaceInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xh.business.domain.req.interfaceinfo.InterfaceInfoQueryRequest;
import com.xh.business.domain.resp.api.InterfaceInfoVO;
import java.util.List;

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

    /**
     * 获取申请接口列表
     *
     * @param page                      分页参数
     * @param interfaceInfoQueryRequest 查询参数
     * @return 列表
     */
    List<InterfaceInfoVO> applyInterfaceList(Page<InterfaceInfoVO> page,
                                             InterfaceInfoQueryRequest interfaceInfoQueryRequest);
}
