package com.xh.business.domain.req.user;

import com.xh.common.core.web.PageQuery;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: QiMu
 * @Date: 2023/09/04 11:34:17
 * @Version: 1.0
 * @Description: 用户查询请求
 */
@Data
public class UserQueryRequest implements Serializable {
    /**
     * id
     */
    private Long id;
    /**
     * 用户昵称
     */
    private String userName;
    /**
     * 账号
     */
    private String userAccount;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 性别
     */
    private String gender;
    /**
     * 用户角色: user, admin
     */
    private String userRole;
}