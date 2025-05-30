package com.xh.business.controller.inner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xh.business.domain.constant.PointsConstant;
import com.xh.business.domain.model.ApiDailyCheckIn;
import com.xh.business.domain.resp.user.UserVO;
import com.xh.business.service.ApiDailyCheckInService;
import com.xh.business.service.ApiUserService;
import com.xh.business.utils.BusinessException;
import com.xh.business.utils.ErrorCode;
import com.xh.business.utils.RedissonLockUtil;
import com.xh.common.core.web.RestResponse;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: QiMu
 * @Date: 2023/08/31 11:51:14
 * @Version: 1.0
 * @Description: 签到接口
 */
@RestController
@RequestMapping("/api/dailyCheckIn")
@Slf4j
public class DailyCheckInController {

    @Resource
    private ApiDailyCheckInService dailyCheckInService;
    @Resource
    private ApiUserService userService;
    @Resource
    private RedissonLockUtil redissonLockUtil;

    // region 增删改查

    /**
     * 签到
     *
     * @param request 请求
     * @return {@link RestResponse}<{@link Boolean}>
     */
    @PostMapping("/doCheckIn")
    @Transactional(rollbackFor = Exception.class)
    public RestResponse<Boolean> doDailyCheckIn(HttpServletRequest request) {
        UserVO loginUser = userService.getLoginUser(request);
        String redissonLock = ("doDailyCheckIn_" + loginUser.getUserAccount()).intern();
        return redissonLockUtil.redissonDistributedLocks(redissonLock, () -> {
            LambdaQueryWrapper<ApiDailyCheckIn> dailyCheckInLambdaQueryWrapper = new LambdaQueryWrapper<>();
            dailyCheckInLambdaQueryWrapper.eq(ApiDailyCheckIn::getUserId, loginUser.getId());
            ApiDailyCheckIn dailyCheckIn = dailyCheckInService.getOne(dailyCheckInLambdaQueryWrapper);
            if (ObjectUtils.isNotEmpty(dailyCheckIn)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "签到失败,今日已签到");
            }
            dailyCheckIn = new ApiDailyCheckIn();
            dailyCheckIn.setUserId(loginUser.getId());
            dailyCheckIn.setAddPoints(10L);
            boolean dailyCheckInResult = dailyCheckInService.save(dailyCheckIn);
            boolean addWalletBalance = userService.addWalletBalance(loginUser.getId(), dailyCheckIn.getAddPoints(), PointsConstant.DAILY_SIGN_IN);
            boolean result = dailyCheckInResult & addWalletBalance;
            if (!result) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }
            return RestResponse.success(true);
        }, "签到失败");
    }
    // endregion
}
