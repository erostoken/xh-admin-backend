package com.xh.business.domain.req.user;

import java.io.Serializable;
import lombok.Data;

/**
 * @Author: QiMu
 * @Date: 2023/09/04 11:34:12
 * @Version: 1.0
 * @Description: 用户登录请求体
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;
}
