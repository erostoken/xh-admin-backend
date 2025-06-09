package com.xh.business.controller.ex;

import cn.dev33.satoken.annotation.SaIgnore;
import com.xh.business.ddd.user.ExApiUserAggregate;
import com.xh.business.domain.model.ApiUser;
import com.xh.business.domain.req.user.UserLoginRequest;
import com.xh.business.domain.req.user.UserRegisterRequest;
import com.xh.business.domain.resp.user.UserVO;
import com.xh.business.utils.BusinessException;
import com.xh.business.utils.ErrorCode;
import com.xh.common.core.configuration.ExUser;
import com.xh.common.core.web.RestResponse;
import com.xh.system.client.dto.ImageCaptchaDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author xuwentao
 * @version 1.0
 * @date 2025/5/29
 * @description 外部用户
 */
@ExUser
@RestController
@RequestMapping("/api/ex/user")
@Slf4j
@Tag(name = "外部API用户")
public class ExUserController {

    @Resource
    private ExApiUserAggregate exApiUserAggregate;

    /**
     * 获取图形验证码
     *
     * @param captchaKey 验证码密钥
     * @return {@link RestResponse}<{@link ImageCaptchaDTO}>
     */
//    @SaIgnore
    @Operation(description = "获取图形验证码")
    @GetMapping("/captcha")
    public RestResponse<ImageCaptchaDTO> getImageCaptcha(String captchaKey) {
        return RestResponse.success(exApiUserAggregate.getImageCaptcha(captchaKey));
    }

    /**
     * API用户注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return {@link RestResponse}<{@link Long}>
     */
    @SaIgnore
    @Operation(description = "API用户注册")
    @PostMapping("/register")
    public RestResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = exApiUserAggregate.userRegister(userRegisterRequest);
        return RestResponse.success(result);
    }

    /**
     * API用户登录
     *
     * @param userLoginRequest 用户登录请求
     * @param request          请求
     * @return {@link RestResponse}<{@link ApiUser}>
     */
    @SaIgnore
    @Operation(description = "API用户登录")
    @PostMapping("/login")
    public RestResponse<UserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        return RestResponse.success(exApiUserAggregate.userLogin(userAccount, userPassword, request));
    }

    /**
     * 获取用户
     *
     * @return {@link RestResponse}<{@link UserVO}>
     */
    @SaIgnore
    @Operation(description = "获取用户")
    @GetMapping("/get")
    public RestResponse<UserVO> getUser() {
        return RestResponse.success(exApiUserAggregate.getLoginUser());
    }

    /**
     * 用户注销
     *
     * @return {@link RestResponse}<{@link Void}>
     */
    @SaIgnore
    @Operation(description = "API用户注销")
    @PostMapping("/logout")
    public RestResponse<Void> userLogout() {
        exApiUserAggregate.userLogout();
        return RestResponse.success();
    }

}
