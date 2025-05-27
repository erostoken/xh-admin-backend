package com.xh.business.domain.req.user;

import java.io.Serializable;
import lombok.Data;

/**
 * @Author: QiMu
 * @Date: 2023/09/04 11:34:06
 * @Version: 1.0
 * @Description: 用户登录请求体
 */
@Data
public class UserEmailLoginRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 邮箱账号
     */
    private String emailAccount;

    /**
     * 验证码
     */
    private String captcha;
}
