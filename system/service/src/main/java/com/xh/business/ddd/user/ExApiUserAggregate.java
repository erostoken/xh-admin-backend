package com.xh.business.ddd.user;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CaptchaUtil;
import static com.xh.business.domain.constant.UserConstant.SALT;
import com.xh.business.domain.enums.UserAccountStatusEnum;
import com.xh.business.domain.model.ApiUser;
import com.xh.business.domain.req.user.UserRegisterRequest;
import com.xh.business.domain.resp.user.UserVO;
import com.xh.business.service.ApiUserService;
import com.xh.business.utils.BusinessException;
import com.xh.business.utils.ErrorCode;
import com.xh.common.core.Constant;
import com.xh.common.core.dto.ExUserInfoDTO;
import com.xh.common.core.utils.LoginUtil;
import com.xh.common.core.web.RestResponse;
import com.xh.system.client.dto.ImageCaptchaDTO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

/**
 * @author xuwentao
 * @version 1.0
 * @date 2025/5/30
 * @description 外部API用户聚合层
 */
@Slf4j
@Component
public class ExApiUserAggregate {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ApiUserService apiUserService;

    /**
     * 获取图片验证码
     *
     * @param captchaKey 验证码密钥
     * @return {@link ImageCaptchaDTO}
     */
    public ImageCaptchaDTO getImageCaptcha(String captchaKey) {
        //定义图形验证码的长、宽、验证码字符数、干扰元素个数
        AbstractCaptcha captcha = CaptchaUtil.createLineCaptcha(100, 30, 4, 10);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        ImageCaptchaDTO imageCaptcha = new ImageCaptchaDTO();
        imageCaptcha.setCaptchaKey(captchaKey);
        imageCaptcha.setImageBase64(captcha.getImageBase64Data());
        valueOperations.set(Constant.CAPTCHA_KEY_PREFIX + captchaKey, captcha, 2, TimeUnit.MINUTES);
        return imageCaptcha;
    }

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return 新用户 id
     */
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        return apiUserService.userRegister(userRegisterRequest);
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request      请求
     * @return 脱敏后的用户信息
     */
    public UserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 如果已经登陆，直接返回
        if(StpUtil.isLogin()) {
            return this.getLoginUser();
        }
        UserVO userVO = new UserVO();

        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短,不能小于4位");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短,不能低于8位字符");
        }
        //  5. 账户不包含特殊字符
        // 匹配由数字、小写字母、大写字母组成的字符串,且字符串的长度至少为1个字符
        String pattern = "[0-9a-zA-Z]+";
        if (!userAccount.matches(pattern)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号需由数字、小写字母、大写字母组成");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        ApiUser user = apiUserService.lambdaQuery()
                .eq(ApiUser::getUserAccount, userAccount)
                .eq(ApiUser::getUserPassword, encryptPassword)
                .one();
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        if (user.getStatus().equals(UserAccountStatusEnum.BAN.getValue())) {
            throw new BusinessException(ErrorCode.PROHIBITED, "账号已封禁");
        }
        BeanUtils.copyProperties(user, userVO);

        // 登录
        StpUtil.login(userVO.getSecretKey(), "WEB");
        SaSession session = StpUtil.getTokenSession();

        // 存储用户信息
        ExUserInfoDTO userInfoDTO = new ExUserInfoDTO();
        userInfoDTO.setToken(StpUtil.getTokenValue());
        userInfoDTO.setAutoRenewal(Boolean.TRUE);
        userInfoDTO.setUserKey(userVO.getUserAccount());
        session.set(LoginUtil.EX_USER_KEY, userInfoDTO);
        BeanUtils.copyProperties(userInfoDTO, userVO);

        // 3. 记录用户的登录态
        return userVO;
    }

    /**
     * 用户注销
     *
     * @return {@link Void}
     */
    public void userLogout() {
        StpUtil.logout();
    }

    /**
     * 获取用户
     *
     * @return {@link RestResponse}<{@link UserVO}>
     */
    public UserVO getLoginUser() {
        return apiUserService.getUser();
    }
}
