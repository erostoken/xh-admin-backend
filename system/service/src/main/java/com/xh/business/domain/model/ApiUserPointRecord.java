package com.xh.business.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 用户积分明细
 * @TableName api_user_point_record
 */
@TableName(value ="api_user_point_record")
@Data
public class ApiUserPointRecord {
    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 操作积分
     */
    private Long points;

    /**
     * 操作渠道
     */
    private String channel;

    /**
     * 操作备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;
}