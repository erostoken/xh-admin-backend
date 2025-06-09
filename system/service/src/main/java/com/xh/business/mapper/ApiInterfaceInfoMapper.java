package com.xh.business.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xh.business.domain.model.ApiInterfaceInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xh.business.domain.req.interfaceinfo.InterfaceInfoQueryRequest;
import com.xh.business.domain.resp.api.InterfaceInfoVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
* @author apple
* @description 针对表【api_interface_info(接口信息)】的数据库操作Mapper
* @createDate 2025-05-26 16:55:21
* @Entity generator.domain.ApiInterfaceInfo
*/
public interface ApiInterfaceInfoMapper extends BaseMapper<ApiInterfaceInfo> {

    /**
     * 获取申请接口列表
     *
     * @param page                      分页参数
     * @param interfaceInfoQueryRequest 查询参数
     * @return 列表
     */
    List<InterfaceInfoVO> applyInterfaceList(Page<InterfaceInfoVO> page,
                                             @Param("param") InterfaceInfoQueryRequest interfaceInfoQueryRequest);
}




