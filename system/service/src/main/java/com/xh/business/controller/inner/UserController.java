package com.xh.business.controller.inner;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.xh.business.config.EmailConfig;
import static com.xh.business.domain.constant.EmailConstant.CAPTCHA_CACHE_KEY;
import static com.xh.business.domain.constant.EmailConstant.EMAIL_HTML_CONTENT_PATH;
import static com.xh.business.domain.constant.EmailConstant.EMAIL_SUBJECT;
import static com.xh.business.domain.constant.EmailConstant.EMAIL_TITLE;
import com.xh.business.domain.enums.UserAccountStatusEnum;
import com.xh.business.domain.model.ApiUser;
import com.xh.business.domain.model.ApiUserPointRecord;
import com.xh.business.domain.req.user.UserAddRequest;
import com.xh.business.domain.req.user.UserBindEmailRequest;
import com.xh.business.domain.req.user.UserEmailLoginRequest;
import com.xh.business.domain.req.user.UserEmailRegisterRequest;
import com.xh.business.domain.req.user.UserLoginRequest;
import com.xh.business.domain.req.user.UserPointsRequest;
import com.xh.business.domain.req.user.UserQueryRequest;
import com.xh.business.domain.req.user.UserRegisterRequest;
import com.xh.business.domain.req.user.UserUnBindEmailRequest;
import com.xh.business.domain.req.user.UserUpdateRequest;
import com.xh.business.domain.resp.user.UserVO;
import com.xh.business.service.ApiUserService;
import com.xh.business.utils.BusinessException;
import com.xh.business.utils.EmailUtil;
import com.xh.business.utils.ErrorCode;
import com.xh.common.core.dto.ExUserInfoDTO;
import com.xh.common.core.dto.OnlineUserDTO;
import com.xh.common.core.utils.LoginUtil;
import com.xh.common.core.web.DeleteRequest;
import com.xh.common.core.web.IdRequest;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口
 *
 * @author qimu
 */
@RestController
@RequestMapping("/api/user")
@Slf4j
@Tag(name = "API用户")
public class UserController {
    @Resource
    private EmailConfig emailConfig;
    @Resource
    private ApiUserService userService;
    @Resource
    private JavaMailSender mailSender;
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    // region 登录相关

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return {@link RestResponse}<{@link Long}>
     */
    @PostMapping("/register")
    public RestResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(userRegisterRequest);
        return RestResponse.success(result);
    }

    /**
     * 用户电子邮件登录
     *
     * @param userEmailLoginRequest 用户登录请求
     * @param request               请求
     * @return {@link RestResponse}<{@link ApiUser}>
     */
    @PostMapping("/email/login")
    public RestResponse<UserVO> userEmailLogin(@RequestBody UserEmailLoginRequest userEmailLoginRequest, HttpServletRequest request) {
        if (userEmailLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserVO user = userService.userEmailLogin(userEmailLoginRequest, request);
        redisTemplate.delete(CAPTCHA_CACHE_KEY + userEmailLoginRequest.getEmailAccount());
        return RestResponse.success(user);
    }

    /**
     * 用户绑定电子邮件
     *
     * @param request              请求
     * @param userBindEmailRequest 用户绑定电子邮件请求
     * @return {@link RestResponse}<{@link UserVO}>
     */
    @PostMapping("/bind/login")
    public RestResponse<UserVO> userBindEmail(@RequestBody UserBindEmailRequest userBindEmailRequest, HttpServletRequest request) {
        if (userBindEmailRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserVO user = userService.userBindEmail(userBindEmailRequest, request);
        return RestResponse.success(user);
    }

    /**
     * 用户取消绑定电子邮件
     *
     * @param request                请求
     * @param userUnBindEmailRequest 用户取消绑定电子邮件请求
     * @return {@link RestResponse}<{@link UserVO}>
     */
    @PostMapping("/unbindEmail")
    public RestResponse<UserVO> userUnBindEmail(@RequestBody UserUnBindEmailRequest userUnBindEmailRequest, HttpServletRequest request) {
        if (userUnBindEmailRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserVO user = userService.userUnBindEmail(userUnBindEmailRequest, request);
        redisTemplate.delete(CAPTCHA_CACHE_KEY + userUnBindEmailRequest.getEmailAccount());
        return RestResponse.success(user);
    }

    /**
     * 用户电子邮件注册
     *
     * @param userEmailRegisterRequest 用户电子邮件注册请求
     * @return {@link RestResponse}<{@link UserVO}>
     */
    @PostMapping("/email/register")
    public RestResponse<Long> userEmailRegister(@RequestBody UserEmailRegisterRequest userEmailRegisterRequest) {
        if (userEmailRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userEmailRegister(userEmailRegisterRequest);
        redisTemplate.delete(CAPTCHA_CACHE_KEY + userEmailRegisterRequest.getEmailAccount());
        return RestResponse.success(result);
    }

    /**
     * 获取验证码
     *
     * @param emailAccount 电子邮件帐户
     * @return {@link RestResponse}<{@link String}>
     */
    @GetMapping("/getCaptcha")
    public RestResponse<Boolean> getCaptcha(String emailAccount) {
        if (StringUtils.isBlank(emailAccount)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        if (!Pattern.matches(emailPattern, emailAccount)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不合法的邮箱地址！");
        }
        String captcha = RandomUtil.randomNumbers(6);
        try {
            sendEmail(emailAccount, captcha);
            redisTemplate.opsForValue().set(CAPTCHA_CACHE_KEY + emailAccount, captcha, 5, TimeUnit.MINUTES);
            return RestResponse.success(true);
        } catch (Exception e) {
            log.error("【发送验证码失败】" + e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "验证码获取失败");
        }
    }

    private void sendEmail(String emailAccount, String captcha) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        // 邮箱发送内容组成
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setSubject(EMAIL_SUBJECT);
        helper.setText(EmailUtil.buildEmailContent(EMAIL_HTML_CONTENT_PATH, captcha), true);
        helper.setTo(emailAccount);
        helper.setFrom(EMAIL_TITLE + '<' + emailConfig.getEmailFrom() + '>');
        mailSender.send(message);
    }

    /**
     * 获取当前登录用户
     *
     * @param request 请求
     * @return {@link RestResponse}<{@link UserVO}>
     */
    @GetMapping("/get/login")
    public RestResponse<UserVO> getLoginUser(HttpServletRequest request) {
        UserVO user = userService.getLoginUser(request);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return RestResponse.success(userVO);
    }

    // endregion

    // region 增删改查

    /**
     * 添加用户
     *
     * @param userAddRequest 用户添加请求
     * @param request        请求
     * @return {@link RestResponse}<{@link Long}>
     */
    @Operation(description = "添加用户")
    @PostMapping("/add")
    public RestResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest, HttpServletRequest request) {
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ApiUser user = new ApiUser();
        BeanUtils.copyProperties(userAddRequest, user);
        // 校验
        userService.validUser(user, true);

        // 调用用户注册
        UserRegisterRequest userRegisterRequest = new UserRegisterRequest();
        userRegisterRequest.setUserAccount(userAddRequest.getUserAccount());
        userRegisterRequest.setUserPassword(userAddRequest.getUserPassword());
        userRegisterRequest.setUserName(userAddRequest.getUserName());
        userRegisterRequest.setCheckPassword(userAddRequest.getUserPassword());
        userService.userRegister(userRegisterRequest);
        return RestResponse.success();
    }

    /**
     * 积分变更
     *
     * @param userPointsRequest 用户添加请求
     * @return {@link RestResponse}<{@link Boolean}>
     */
    @Operation(description = "积分变更")
    @PostMapping("/points/change")
    public RestResponse<Boolean> pointsChange(@RequestBody UserPointsRequest userPointsRequest) {
        if (userPointsRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return RestResponse.success(userService.pointsChange(userPointsRequest));
    }

    /**
     * 用户积分分页
     *
     * @param userQueryRequest 用户积分请求
     * @return {@link RestResponse}<{@link Page}<{@link ApiUserPointRecord}>>
     */
    @Operation(description = "用户积分分页")
    @PostMapping("/points/page")
    public RestResponse<Page<ApiUserPointRecord>> pointsPage(@RequestBody PageQuery<UserQueryRequest> userQueryRequest) {
        if (ObjectUtils.anyNull(userQueryRequest, userQueryRequest.getParam(), userQueryRequest.getParam().getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return RestResponse.success(userService.pointsPage(userQueryRequest));
    }

    /**
     * 删除用户
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return {@link RestResponse}<{@link Boolean}>
     */
    @Operation(description = "删除用户")
    @PostMapping("/delete")
    public RestResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (ObjectUtils.anyNull(deleteRequest, deleteRequest.getId()) || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return RestResponse.success(userService.removeById(deleteRequest.getId()));
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest 用户更新请求
     * @return {@link RestResponse}<{@link ApiUser}>
     */
    @Operation(description = "更新用户")
    @PostMapping("/update")
    @Transactional(rollbackFor = Exception.class)
    public RestResponse<UserVO> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (ObjectUtils.anyNull(userUpdateRequest, userUpdateRequest.getId()) || userUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 管理员才能操作
        boolean adminOperation = ObjectUtils.anyNull(userUpdateRequest.getBalance(),
                userUpdateRequest.getUserRole(), userUpdateRequest.getUserPassword());
        // 校验是否登录
        OnlineUserDTO onlineUserInfo = LoginUtil.getOnlineUserInfo();
        // 处理管理员业务,不是管理员抛异常
        if (adminOperation && Boolean.FALSE.equals(onlineUserInfo.isAdmin())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        if (Boolean.FALSE.equals(onlineUserInfo.isAdmin())
                && !userUpdateRequest.getId().equals(onlineUserInfo.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有本人或管理员可以修改");
        }

        ApiUser user = new ApiUser();
        BeanUtils.copyProperties(userUpdateRequest, user);
        // 参数校验
        userService.validUser(user, false);

        LambdaUpdateWrapper<ApiUser> userLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        userLambdaUpdateWrapper.eq(ApiUser::getId, user.getId());
        boolean result = userService.update(user, userLambdaUpdateWrapper);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新失败");
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userService.getById(user.getId()), userVO);
        return RestResponse.success(userVO);
    }

    /**
     * 根据 id 获取用户
     *
     * @param id      id
     * @param request 请求
     * @return {@link RestResponse}<{@link UserVO}>
     */
    @Operation(description = "根据 id 获取用户")
    @GetMapping("/get")
    public RestResponse<UserVO> getUserById(@RequestParam int id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ApiUser user = userService.getById(id);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return RestResponse.success(userVO);
    }

    /**
     * 获取用户列表
     *
     * @param userQueryRequest 用户查询请求
     * @param request          请求
     * @return {@link RestResponse}<{@link List}<{@link UserVO}>>
     */
    @Operation(description = "获取用户列表")
    @GetMapping("/list")
    public RestResponse<List<UserVO>> listUser(UserQueryRequest userQueryRequest, HttpServletRequest request) {
        if (null == userQueryRequest) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ApiUser userQuery = new ApiUser();
        BeanUtils.copyProperties(userQueryRequest, userQuery);

        QueryWrapper<ApiUser> queryWrapper = new QueryWrapper<>(userQuery);
        List<ApiUser> userList = userService.list(queryWrapper);
        List<UserVO> userVOList = userList.stream().map(user -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            return userVO;
        }).collect(Collectors.toList());
        return RestResponse.success(userVOList);
    }

    /**
     * 分页获取用户列表
     *
     * @param userQueryRequest 用户查询请求
     * @param request          请求
     * @return {@link RestResponse}<{@link Page}<{@link UserVO}>>
     */
    @Operation(description = "分页获取用户列表")
    @PostMapping("/list/page")
    public RestResponse<Page<UserVO>> listUserByPage(@RequestBody PageQuery<UserQueryRequest> userQueryRequest, HttpServletRequest request) {
        ApiUser userQuery = new ApiUser();
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 将条件复制给query
        BeanUtils.copyProperties(userQueryRequest.getParam(), userQuery);
        long current = userQueryRequest.getCurrentPage();
        long pageSize = userQueryRequest.getPageSize();

        String userName = userQuery.getUserName();
        String userEmail = userQuery.getEmail();
        String userAccount = userQuery.getUserAccount();
        String gender = userQuery.getGender();
        String userRole = userQuery.getUserRole();
        Page<ApiUser> userPage = userService.page(new Page<>(current, pageSize), Wrappers.<ApiUser>lambdaQuery()
                .like(StringUtils.isNotBlank(userName), ApiUser::getUserName, userName)
                .like(StringUtils.isNotBlank(userEmail), ApiUser::getEmail, userEmail)
                .like(StringUtils.isNotBlank(userAccount), ApiUser::getUserAccount, userAccount)
                .eq(StringUtils.isNotBlank(gender), ApiUser::getGender, gender)
                .eq(StringUtils.isNotBlank(userRole), ApiUser::getUserRole, userRole));
        Page<UserVO> userVoPage = new PageDTO<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<UserVO> userVOList = userPage.getRecords().stream().map(user -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            return userVO;
        }).collect(Collectors.toList());
        userVoPage.setRecords(userVOList);
        return RestResponse.success(userVoPage);
    }

    @PostMapping("/update/voucher")
    public RestResponse<UserVO> updateVoucher(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserVO loginUser = userService.getLoginUser(request);
        ApiUser user = new ApiUser();
        BeanUtils.copyProperties(loginUser, user);
        UserVO userVO = userService.updateVoucher(user);
        return RestResponse.success(userVO);
    }

    /**
     * 通过邀请码获取用户
     *
     * @param invitationCode 邀请码
     * @return {@link RestResponse}<{@link UserVO}>
     */
    @PostMapping("/get/invitationCode")
    public RestResponse<UserVO> getUserByInvitationCode(String invitationCode) {
        if (StringUtils.isBlank(invitationCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LambdaQueryWrapper<ApiUser> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.eq(ApiUser::getInvitationCode, invitationCode);
        ApiUser invitationCodeUser = userService.getOne(userLambdaQueryWrapper);
        if (invitationCodeUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "邀请码不存在");
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(invitationCodeUser, userVO);
        return RestResponse.success(userVO);
    }

    /**
     * 解封
     *
     * @param idRequest id请求
     * @return {@link RestResponse}<{@link Boolean}>
     */
    @Operation(description = "解封")
    @PostMapping("/normal")
    public RestResponse<Boolean> normalUser(@RequestBody IdRequest idRequest) {
        if (ObjectUtils.anyNull(idRequest, idRequest.getId()) || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = idRequest.getId();
        ApiUser user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        user.setStatus(UserAccountStatusEnum.NORMAL.getValue());
        return RestResponse.success(userService.updateById(user));
    }

    /**
     * 封号
     *
     * @param idRequest id请求
     * @param request   请求
     * @return {@link RestResponse}<{@link Boolean}>
     */
    @Operation(description = "封号")
    @PostMapping("/ban")
    public RestResponse<Boolean> banUser(@RequestBody IdRequest idRequest, HttpServletRequest request) {
        if (ObjectUtils.anyNull(idRequest, idRequest.getId()) || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = idRequest.getId();
        ApiUser user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        user.setStatus(UserAccountStatusEnum.BAN.getValue());
        return RestResponse.success(userService.updateById(user));
    }
    // endregion
}
