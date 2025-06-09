package com.xh.business.domain.resp.api;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xuwentao
 * @version 1.0
 * @date 2025/6/6
 * @description 备注信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterfaceInfoVO {
    /**
     * id
     */
    private Long id;

    /**
     * 接口名称
     */
    private String name;

    /**
     * 接口地址
     */
    private String url;

    /**
     * 发布人
     */
    private Long userId;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 接口请求参数
     */
    private String requestParams;

    /**
     * 接口响应参数
     */
    private String responseParams;

    /**
     * 扣除积分数
     */
    private Long reduceScore;

    /**
     * 返回格式(JSON等等)
     */
    private String returnFormat;

    /**
     * 描述信息
     */
    private String description;

    /**
     * 接口状态（0- 默认下线 1- 上线）
     */
    private Integer status;

    /**
     * 接口总调用次数
     */
    private Long totalInvokes;

    /**
     * 接口头像
     */
    private String avatarUrl;

    /**
     * 创建时间
     */
    private Date createTime;

    // 业务字段
    /**
     * 申请状态（0- 申请中 1- 已通过 2- 已驳回）
     */
    private Integer applyStatus;
}
