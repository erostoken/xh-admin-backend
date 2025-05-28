package com.xh.business.domain.req.user;

import lombok.Data;

/**
 * @author xuwentao
 * @version 1.0
 * @date 2025/5/28
 * @description 备注信息
 */
@Data
public class UserPointsRequest {

    /**
     * id
     */
    private Long id;
    /**
     * 积分变更方式 - 1: 新增 2:扣减 3:修改
     */
    private Integer type;
    /**
     * 积分变更数量
     */
    private Long points;
    /**
     * 积分变更备注
     */
    private String remark;

}
