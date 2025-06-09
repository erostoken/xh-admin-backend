package com.xh.business.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 用户接口申请表
 * @TableName api_user_interface_apply
 */
@TableName(value ="api_user_interface_apply")
@Data
public class ApiUserInterfaceApply {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 申请人id
     */
    private Long applyId;

    /**
     * 接口id
     */
    private Long interfaceId;

    /**
     * 申请状态（0- 申请中 1- 已通过 2- 已驳回）
     */
    private Integer status;

    /**
     * 申请时间
     */
    private Date applyTime;

    /**
     * 审批时间
     */
    private Date approvalTime;

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