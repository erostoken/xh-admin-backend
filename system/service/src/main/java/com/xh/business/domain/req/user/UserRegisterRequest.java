package com.xh.business.domain.req.user;

import java.io.Serializable;
import lombok.Data;

/**
 * @Author: QiMu
 * @Date: 2023/09/04 11:34:21
 * @Version: 1.0
 * @Description: 用户注册请求体
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 确认密码
     */
    private String checkPassword;

    /**
     * 邀请码
     */
    private String invitationCode;

    /**
     * 同意协议
     */
    private String agreeToAnAgreement;
}
