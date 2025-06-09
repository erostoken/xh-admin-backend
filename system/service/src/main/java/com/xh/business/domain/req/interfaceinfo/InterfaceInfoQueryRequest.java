package com.xh.business.domain.req.interfaceinfo;

import com.xh.common.core.web.PageQuery;
import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: QiMu
 * @Date: 2023/09/04 11:33:30
 * @Version: 1.0
 * @Description: 查询请求
 */
@Data
public class InterfaceInfoQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关键词 包括名称、描述
     */
    private String keyword;
    /**
     * 分类ID
     */
    private String categoryCode;
    /**
     * 申请状态（0- 申请中 1- 已通过 2- 已驳回）
     */
    private Integer applyStatus;
    /**
     * 申请人
     */
    private Long applyUserId;
    /**
     * 接口名称
     */
    private String name;
    /**
     * 返回格式
     */
    private String returnFormat;
    /**
     * 接口地址
     */
    private String url;
    /**
     * 接口响应参数
     */
    private List<ResponseParamsField> responseParams;
    /**
     * 发布人
     */
    private Long userId;
    /**
     * 减少积分个数
     */
    private Integer reduceScore;
    /**
     * 请求方法
     */
    private String method;
    /**
     * 描述信息
     */
    private String description;
    /**
     * 接口状态（0- 默认下线 1- 上线）
     */
    private Integer status;
}