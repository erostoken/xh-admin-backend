package com.xh.business.domain.req.user;

import java.io.Serializable;
import lombok.Data;

/**
 * @Author: QiMu
 * @Date: 2023/09/04 11:34:09
 * @Version: 1.0
 * @Description: 用户注册请求体
 */
@Data
public class UserEmailRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 邮箱账号
     */
    private String emailAccount;

    /**
     * 验证码
     */
    private String captcha;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 邀请码
     */
    private String invitationCode;

    /**
     * 同意协议
     */
    private String agreeToAnAgreement;
}
