package com.xh.business.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 每日签到表
 * @TableName api_daily_check_in
 */
@TableName(value ="api_daily_check_in")
@Data
public class ApiDailyCheckIn {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 签到人
     */
    private Long userId;

    /**
     * 描述
     */
    private String description;

    /**
     * 签到增加积分个数
     */
    private Long addPoints;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}