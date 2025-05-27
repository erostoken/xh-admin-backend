package com.xh.business.domain.req.user;

import java.io.Serializable;
import lombok.Data;

/**
 * @Author: QiMu
 * @Date: 2023/09/20 11:42:22
 * @Version: 1.0
 * @Description: 用户取消绑定电子邮件请求
 */
@Data
public class UserUnBindEmailRequest implements Serializable {

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
