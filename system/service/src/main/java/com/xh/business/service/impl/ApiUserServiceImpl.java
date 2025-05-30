package com.xh.business.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import static com.xh.business.domain.constant.EmailConstant.CAPTCHA_CACHE_KEY;
import com.xh.business.domain.constant.PointsConstant;
import com.xh.business.domain.enums.UserAccountStatusEnum;
import com.xh.business.domain.model.ApiUser;
import com.xh.business.domain.model.ApiUserPointRecord;
import com.xh.business.domain.req.user.UserBindEmailRequest;
import com.xh.business.domain.req.user.UserEmailLoginRequest;
import com.xh.business.domain.req.user.UserEmailRegisterRequest;
import com.xh.business.domain.req.user.UserPointsRequest;
import com.xh.business.domain.req.user.UserQueryRequest;
import com.xh.business.domain.req.user.UserRegisterRequest;
import com.xh.business.domain.req.user.UserUnBindEmailRequest;
import com.xh.business.domain.resp.user.UserVO;
import com.xh.business.mapper.ApiUserMapper;
import com.xh.business.service.ApiUserPointRecordService;
import com.xh.business.service.ApiUserService;
import com.xh.business.utils.BusinessException;
import com.xh.business.utils.ErrorCode;
import com.xh.business.utils.RedissonLockUtil;
import com.xh.common.core.Constant;
import com.xh.common.core.dto.ExUserInfoDTO;
import com.xh.common.core.dto.SysLoginUserInfoDTO;
import com.xh.common.core.dto.SysUserDTO;
import com.xh.common.core.utils.LoginUtil;
import com.xh.common.core.web.PageQuery;
import com.xh.system.client.dto.ImageCaptchaDTO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import static com.xh.business.domain.constant.UserConstant.*;

/**
* @author apple
* @description 针对表【api_user(用户)】的数据库操作Service实现
* @createDate 2025-05-26 16:55:21
*/
@Slf4j
@Service
public class ApiUserServiceImpl extends ServiceImpl<ApiUserMapper, ApiUser>
    implements ApiUserService {
    
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RedissonLockUtil redissonLockUtil;
    @Resource
    private ApiUserPointRecordService apiUserPointRecordService;

    /**
     * 用户寄存器
     *
     * @param userRegisterRequest 用户注册请求
     * @return long
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String userName = StringUtils.isBlank(userRegisterRequest.getUserName()) ?
                String.format("注册用户%s", userAccount) : userRegisterRequest.getUserName();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String invitationCode = userRegisterRequest.getInvitationCode();

        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userName.length() > 40) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称过长");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        //  5. 账户不包含特殊字符
        // 匹配由数字、小写字母、大写字母组成的字符串,且字符串的长度至少为1个字符
        String pattern = "[0-9a-zA-Z]+";
        if (!userAccount.matches(pattern)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号由数字、小写字母、大写字母组成");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        String redissonLock = ("userRegister_" + userAccount).intern();
        return redissonLockUtil.redissonDistributedLocks(redissonLock, () -> {
            // 账户不能重复
            long count = this.lambdaQuery().eq(ApiUser::getUserAccount, userAccount).count();
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            ApiUser invitationCodeUser = null;
            if (StringUtils.isNotBlank(invitationCode)) {
                LambdaQueryWrapper<ApiUser> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
                userLambdaQueryWrapper.eq(ApiUser::getInvitationCode, invitationCode);
                // 可能出现重复invitationCode,查出的不是一条
                invitationCodeUser = this.getOne(userLambdaQueryWrapper);
                if (invitationCodeUser == null) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "该邀请码无效");
                }
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // ak/sk
            String accessKey = DigestUtils.md5DigestAsHex((userAccount + SALT + VOUCHER).getBytes());
            String secretKey = DigestUtils.md5DigestAsHex((SALT + VOUCHER + userAccount).getBytes());

            // 3. 插入数据
            ApiUser user = new ApiUser();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            user.setUserName(userName);
            user.setAccessKey(accessKey);
            user.setSecretKey(secretKey);
            user.setBalance(0L);
            if (invitationCodeUser != null) {
                user.setBalance(100L);
                this.addWalletBalance(invitationCodeUser.getId(), 100L);
            }
            user.setInvitationCode(generateRandomString(8));
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }, "注册账号失败");
    }


    /**
     * 用户电子邮件注册
     *
     * @param userEmailRegisterRequest 用户电子邮件注册请求
     * @return long
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long userEmailRegister(UserEmailRegisterRequest userEmailRegisterRequest) {
        String emailAccount = userEmailRegisterRequest.getEmailAccount();
        String captcha = userEmailRegisterRequest.getCaptcha();
        String userName = userEmailRegisterRequest.getUserName();
        String invitationCode = userEmailRegisterRequest.getInvitationCode();

        if (StringUtils.isAnyBlank(emailAccount, captcha)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (userName.length() > 40) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称过长");
        }
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        if (!Pattern.matches(emailPattern, emailAccount)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不合法的邮箱地址！");
        }
        String cacheCaptcha = (String) redisTemplate.opsForValue().get(CAPTCHA_CACHE_KEY + emailAccount);
        if (StringUtils.isBlank(cacheCaptcha)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "验证码已过期,请重新获取");
        }
        captcha = captcha.trim();
        if (!cacheCaptcha.equals(captcha)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "验证码输入有误");
        }
        String redissonLock = ("userEmailRegister_" + emailAccount).intern();
        return redissonLockUtil.redissonDistributedLocks(redissonLock, () -> {
            // 账户不能重复
            QueryWrapper<ApiUser> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", emailAccount);
            long count = this.getBaseMapper().selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            ApiUser invitationCodeUser = null;
            if (StringUtils.isNotBlank(invitationCode)) {
                LambdaQueryWrapper<ApiUser> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
                userLambdaQueryWrapper.eq(ApiUser::getInvitationCode, invitationCode);
                // 可能出现重复invitationCode,查出的不是一条
                invitationCodeUser = this.getOne(userLambdaQueryWrapper);
                if (invitationCodeUser == null) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "该邀请码无效");
                }
            }
            // ak/sk
            String accessKey = DigestUtils.md5DigestAsHex((Arrays.toString(RandomUtil.randomBytes(10)) + SALT + VOUCHER).getBytes());
            String secretKey = DigestUtils.md5DigestAsHex((SALT + VOUCHER + Arrays.toString(RandomUtil.randomBytes(10))).getBytes());

            // 3. 插入数据
            ApiUser user = new ApiUser();
            user.setUserAccount(emailAccount);
            user.setUserName(userName);
            user.setAccessKey(accessKey);
            user.setEmail(emailAccount);
            user.setSecretKey(secretKey);
            if (invitationCodeUser != null) {
                user.setBalance(100L);
                this.addWalletBalance(invitationCodeUser.getId(), 100L);
            }
            user.setInvitationCode(generateRandomString(8));
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }, "邮箱账号注册失败");
    }

    /**
     * 用户电子邮件登录
     *
     * @param userEmailLoginRequest 用户电子邮件登录请求
     * @param request               要求
     * @return {@link UserVO}
     */
    @Override
    public UserVO userEmailLogin(UserEmailLoginRequest userEmailLoginRequest, HttpServletRequest request) {
        String emailAccount = userEmailLoginRequest.getEmailAccount();
        String captcha = userEmailLoginRequest.getCaptcha();

        if (StringUtils.isAnyBlank(emailAccount, captcha)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        if (!Pattern.matches(emailPattern, emailAccount)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不合法的邮箱地址！");
        }
        String cacheCaptcha = (String) redisTemplate.opsForValue().get(CAPTCHA_CACHE_KEY + emailAccount);
        if (StringUtils.isBlank(cacheCaptcha)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "验证码已过期,请重新获取");
        }
        captcha = captcha.trim();
        if (!cacheCaptcha.equals(captcha)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "验证码输入有误");
        }
        // 查询用户是否存在
        QueryWrapper<ApiUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", emailAccount);
        ApiUser user = this.getBaseMapper().selectOne(queryWrapper);

        // 用户不存在
        if (user == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该邮箱未绑定账号，请先绑定账号");
        }

        if (user.getStatus().equals(UserAccountStatusEnum.BAN.getValue())) {
            throw new BusinessException(ErrorCode.PROHIBITED, "账号已封禁");
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, userVO);
        return userVO;
    }

    @Override
    public UserVO userBindEmail(UserBindEmailRequest userEmailLoginRequest, HttpServletRequest request) {
        String emailAccount = userEmailLoginRequest.getEmailAccount();
        String captcha = userEmailLoginRequest.getCaptcha();
        if (StringUtils.isAnyBlank(emailAccount, captcha)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        if (!Pattern.matches(emailPattern, emailAccount)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不合法的邮箱地址！");
        }
        String cacheCaptcha = (String) redisTemplate.opsForValue().get(CAPTCHA_CACHE_KEY + emailAccount);
        if (StringUtils.isBlank(cacheCaptcha)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "验证码已过期,请重新获取");
        }
        captcha = captcha.trim();
        if (!cacheCaptcha.equals(captcha)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "验证码输入有误");
        }
        // 查询用户是否绑定该邮箱
        UserVO loginUser = this.getLoginUser(request);
        if (loginUser.getEmail() != null && emailAccount.equals(loginUser.getEmail())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该账号已绑定此邮箱,请更换新的邮箱！");
        }
        // 查询邮箱是否已经绑定
        QueryWrapper<ApiUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", emailAccount);
        ApiUser user = this.getOne(queryWrapper);
        if (user != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "此邮箱已被绑定,请更换新的邮箱！");
        }
        user = new ApiUser();
        user.setId(loginUser.getId());
        user.setEmail(emailAccount);
        boolean bindEmailResult = this.updateById(user);
        if (!bindEmailResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "邮箱绑定失败,请稍后再试！");
        }
        loginUser.setEmail(emailAccount);
        return loginUser;
    }

    @Override
    public UserVO userUnBindEmail(UserUnBindEmailRequest userUnBindEmailRequest, HttpServletRequest request) {
        String emailAccount = userUnBindEmailRequest.getEmailAccount();
        String captcha = userUnBindEmailRequest.getCaptcha();
        if (StringUtils.isAnyBlank(emailAccount, captcha)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        if (!Pattern.matches(emailPattern, emailAccount)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不合法的邮箱地址！");
        }
        String cacheCaptcha = (String) redisTemplate.opsForValue().get(CAPTCHA_CACHE_KEY + emailAccount);
        if (StringUtils.isBlank(cacheCaptcha)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "验证码已过期,请重新获取");
        }
        captcha = captcha.trim();
        if (!cacheCaptcha.equals(captcha)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "验证码输入有误");
        }
        // 查询用户是否绑定该邮箱
        UserVO loginUser = this.getLoginUser(request);
        if (loginUser.getEmail() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该账号未绑定邮箱");
        }
        if (!emailAccount.equals(loginUser.getEmail())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该账号未绑定此邮箱");
        }
        ApiUser user = new ApiUser();
        user.setId(loginUser.getId());
        user.setEmail("");
        boolean bindEmailResult = this.updateById(user);
        if (!bindEmailResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "邮箱解绑失败,请稍后再试！");
        }
        loginUser.setEmail(null);
        return loginUser;
    }

    /**
     * 获取当前登录用户
     *
     * @param request 要求
     * @return {@link ApiUser}
     */
    @Override
    public UserVO getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        UserVO currentUser = (UserVO) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        ApiUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (user.getStatus().equals(UserAccountStatusEnum.BAN.getValue())) {
            throw new BusinessException(ErrorCode.PROHIBITED, "账号已封禁");
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 是否为管理员
     *
     * @param request 要求
     * @return boolean
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        UserVO currentUser = (UserVO) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        ApiUser user = this.getById(userId);
        return user != null && ADMIN_ROLE.equals(user.getUserRole());
    }

    @Override
    public ApiUser isTourist(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        UserVO currentUser = (UserVO) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    @Override
    public void validUser(ApiUser user, boolean add) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = user.getUserAccount();
        String userPassword = user.getUserPassword();
        Long balance = user.getBalance();

        // 创建时，所有参数必须非空
        if (add) {
            if (StringUtils.isAnyBlank(userAccount, userPassword)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
            // 添加用户生成8位邀请码
            user.setInvitationCode(generateRandomString(8));
        }
        //  5. 账户不包含特殊字符
        // 匹配由数字、小写字母、大写字母组成的字符串,且字符串的长度至少为1个字符
        String pattern = "[0-9a-zA-Z]+";
        if (StringUtils.isNotBlank(userAccount) && !userAccount.matches(pattern)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号由数字、小写字母、大写字母组成");
        }
        // 账户不能重复
        if (StringUtils.isNotBlank(userAccount)) {
            long count = this.lambdaQuery()
                    .eq(ApiUser::getUserAccount, userAccount)
                    .ne(Objects.nonNull(user.getId()), ApiUser::getId, user.getId())
                    .count();
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
        }
        if (ObjectUtils.isNotEmpty(balance) && balance < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "钱包余额不能为负数");
        }
        if (StringUtils.isNotBlank(userPassword)) {
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            user.setUserPassword(encryptPassword);
        }
    }

    @Override
    public UserVO updateVoucher(ApiUser loginUser) {
        String accessKey = DigestUtils.md5DigestAsHex((Arrays.toString(RandomUtil.randomBytes(10)) + SALT + VOUCHER).getBytes());
        String secretKey = DigestUtils.md5DigestAsHex((SALT + VOUCHER + Arrays.toString(RandomUtil.randomBytes(10))).getBytes());
        loginUser.setAccessKey(accessKey);
        loginUser.setSecretKey(secretKey);
        boolean result = this.updateById(loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(loginUser, userVO);
        return userVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean pointsChange(UserPointsRequest userPointsRequest) {
        ApiUser apiUser = this.getById(userPointsRequest.getId());
        if (apiUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        String channel = PointsConstant.ADMIN_OPERATION;
        String remark = userPointsRequest.getRemark();
        // 积分变更方式 - 1: 新增 2:扣减 3:修改
        switch (userPointsRequest.getType()) {
            case 1:
                this.addWalletBalance(userPointsRequest.getId(), userPointsRequest.getPoints(), channel, remark);
                break;
            case 2:
                this.reduceWalletBalance(userPointsRequest.getId(), userPointsRequest.getPoints(), channel, remark);
                break;
            case 3:
                long changePoints = apiUser.getBalance() - userPointsRequest.getPoints();
                if(changePoints > 0) {
                    this.addWalletBalance(userPointsRequest.getId(), changePoints, channel, remark);
                } else {
                    this.reduceWalletBalance(userPointsRequest.getId(), changePoints, channel, remark);
                }
                break;
            default:
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return false;
    }

    /**
     * 添加钱包余额
     *
     * @param userId    用户id
     * @param addPoints 添加点
     * @param channel   渠道
     * @param remark    备注
     * @return boolean
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addWalletBalance(Long userId, Long addPoints, String channel, String remark) {
        ApiUserPointRecord apiUserPointRecord = new ApiUserPointRecord();
        apiUserPointRecord.setUserId(userId);
        apiUserPointRecord.setPoints(addPoints);
        apiUserPointRecord.setChannel(channel);
        apiUserPointRecord.setRemark(remark);
        apiUserPointRecordService.save(apiUserPointRecord);

        LambdaUpdateWrapper<ApiUser> userLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        userLambdaUpdateWrapper.eq(ApiUser::getId, userId);
        userLambdaUpdateWrapper.setSql("balance = balance + " + addPoints);
        return this.update(userLambdaUpdateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addWalletBalance(Long userId, Long addPoints, String channel) {
        return this.addWalletBalance(userId, addPoints, channel, "");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addWalletBalance(Long userId, Long addPoints) {
        return this.addWalletBalance(userId, addPoints, PointsConstant.SYSTEM_OPERATION, "");
    }


    /**
     * 减少钱包余额
     *
     * @param userId      用户id
     * @param reduceScore 减少的分数
     * @param channel     渠道
     * @param remark      备注
     * @return boolean
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reduceWalletBalance(Long userId, Long reduceScore, String channel, String remark) {
        ApiUserPointRecord apiUserPointRecord = new ApiUserPointRecord();
        apiUserPointRecord.setUserId(userId);
        apiUserPointRecord.setPoints(-reduceScore);
        apiUserPointRecord.setChannel(channel);
        apiUserPointRecord.setRemark(remark);
        apiUserPointRecordService.save(apiUserPointRecord);

        LambdaUpdateWrapper<ApiUser> userLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        userLambdaUpdateWrapper.eq(ApiUser::getId, userId);
        userLambdaUpdateWrapper.setSql("balance = balance - " + reduceScore);
        return this.update(userLambdaUpdateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reduceWalletBalance(Long userId, Long reduceScore, String channel) {
        return this.reduceWalletBalance(userId, reduceScore, channel, "");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reduceWalletBalance(Long userId, Long reduceScore) {
        return this.reduceWalletBalance(userId, reduceScore, PointsConstant.SYSTEM_OPERATION, "");
    }

    @Override
    public Page<ApiUserPointRecord> pointsPage(PageQuery<UserQueryRequest> userQueryRequest) {
        UserQueryRequest param = userQueryRequest.getParam();

        Long userId = param.getId();
        Page<ApiUserPointRecord> page = new Page<>(userQueryRequest.getCurrentPage(), userQueryRequest.getPageSize());
        return apiUserPointRecordService.page(page, Wrappers.<ApiUserPointRecord>lambdaQuery()
                .eq(ApiUserPointRecord::getUserId, userId));
    }

    @Override
    public UserVO getUser() {
        // 获取sa token用户信息
        ExUserInfoDTO userInfoDTO = StpUtil.getTokenSession().getModel(LoginUtil.EX_USER_KEY, ExUserInfoDTO.class);
        if (Objects.isNull(userInfoDTO) || StringUtils.isBlank(userInfoDTO.getUserKey())) {
            StpUtil.getTokenSession().delete(LoginUtil.EX_USER_KEY);
            return null;
        }

        // 获取用户
        ApiUser user = this.lambdaQuery()
                .eq(ApiUser::getUserAccount, userInfoDTO.getUserKey())
                .one();
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        if (user.getStatus().equals(UserAccountStatusEnum.BAN.getValue())) {
            throw new BusinessException(ErrorCode.PROHIBITED, "账号已封禁");
        }

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        BeanUtils.copyProperties(userInfoDTO, userVO);
        return userVO;
    }

    /**
     * 生成随机字符串
     *
     * @param length 长
     * @return {@link String}
     */
    public String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            char randomChar = characters.charAt(index);
            sb.append(randomChar);
        }
        return sb.toString();
    }
}




