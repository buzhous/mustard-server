package org.jeecg.modules.app.controller.coupon;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.coupon.CouponCreateVO;
import org.jeecg.modules.app.bean.vo.coupon.CouponMyVO;
import org.jeecg.modules.app.bean.vo.coupon.CouponUpdateVO;
import org.jeecg.modules.app.constant.CouponConstant;
import org.jeecg.modules.app.entity.Coupon;
import org.jeecg.modules.app.entity.UserCoupon;
import org.jeecg.modules.app.entity.user.AppUser;
import org.jeecg.modules.app.service.ICouponService;
import org.jeecg.modules.app.service.IUserCouponService;
import org.jeecg.modules.app.utils.AppAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "优惠券管理")
@Slf4j
@RestController
@RequestMapping("/app/coupon")
public class AppCouponController extends JeecgController<Coupon, ICouponService> {

    @Autowired
    private ICouponService couponService;

    @Autowired
    private IUserCouponService userCouponService;

    //@Operation(summary = "优惠券列表")
    @GetMapping(value = "/list")
    public Result<Page<Coupon>> list(Coupon coupon,
                                      @RequestParam(defaultValue = "1") Integer pageNo,
                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Coupon> page = new Page<>(pageNo, pageSize);
        Page<Coupon> result = couponService.queryCouponPage(page, coupon);
        return Result.OK(result);
    }

    //@Operation(summary = "可领取优惠券列表")
    @GetMapping(value = "/available")
    public Result<List<Coupon>> availableCoupons() {
        List<Coupon> coupons = couponService.queryAvailableCoupons();
        return Result.OK(coupons);
    }

    @Operation(summary = "领取优惠券")
    @PostMapping(value = "/receive")
    public Result<Boolean> receiveCoupon(@RequestParam Long couponId) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            throw new AppException(ExceptionEnum.USER_INFO_NOT_EXIST);
        }
        boolean result = userCouponService.receiveCoupon(loginUser.getId(), couponId);
        return Result.OK(result);
    }

    //@Operation(summary = "发布优惠券")
    @PostMapping(value = "/publish")
    public Result<Boolean> publishCoupon(@RequestParam Long couponId) {
        boolean result = couponService.publishCoupon(couponId);
        return Result.OK(result);
    }

    //@Operation(summary = "下架优惠券")
    @PostMapping(value = "/offline")
    public Result<Boolean> offlineCoupon(@RequestParam Long couponId) {
        boolean result = couponService.offlineCoupon(couponId);
        return Result.OK(result);
    }

    //@Operation(summary = "更新优惠券")
    @PostMapping(value = "/update")
    public Result<Boolean> updateCoupon(@RequestBody CouponUpdateVO updateVO) {
        boolean result = couponService.updateCoupon(updateVO);
        return Result.OK(result);
    }

    //@Operation(summary = "删除优惠券")
    @PostMapping(value = "/delete")
    public Result<Boolean> deleteCoupon(@RequestParam Long couponId) {
        boolean result = couponService.deleteCoupon(couponId);
        return Result.OK(result);
    }

    //@Operation(summary = "创建优惠券")
    @PostMapping(value = "/create")
    public Result<Coupon> createCoupon(@RequestBody CouponCreateVO createVO) {
        Coupon coupon = couponService.createCoupon(createVO);
        return Result.OK(coupon);
    }

    //@Operation(summary = "获取优惠券详情")
    @GetMapping(value = "/detail")
    public Result<Coupon> detail(@RequestParam Long couponId) {
        Coupon coupon = couponService.getById(couponId);
        if (ObjectUtil.isEmpty(coupon)) {
            throw new AppException(ExceptionEnum.COUPON_NOT_EXIST);
        }
        return Result.OK(coupon);
    }

    @Operation(summary = "我的拾取券数量")
    @GetMapping(value = "/my")
    public Result<CouponMyVO> my() {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            throw new AppException(ExceptionEnum.USER_INFO_NOT_EXIST);
        }
        CouponMyVO myVO = new CouponMyVO();
        myVO.setAvailableCount(userCouponService.countAvailableRuinsPickupCoupons(loginUser.getId()));
        myVO.setClaimedToday(isClaimedDailyCouponToday(loginUser.getId()));
        return Result.OK(myVO);
    }

    /**
     * 当日是否已领满每日限领数量（领取按钮据此隐藏；券未惰性创建时视为未领取）
     */
    private boolean isClaimedDailyCouponToday(String userId) {
        Coupon coupon = couponService.getByCode(CouponConstant.RUINS_PICKUP_CODE);
        if (ObjectUtil.isEmpty(coupon)) {
            return false;
        }
        int dailyLimit = CouponConstant.resolveDailyClaimLimit(coupon.getDailyLimit());
        return userCouponService.countUserDailyReceivedCoupons(userId, coupon.getId()) >= dailyLimit;
    }

    @Operation(summary = "每日领取拾取券")
    @PostMapping(value = "/claimDaily")
    public Result<UserCoupon> claimDaily() {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            throw new AppException(ExceptionEnum.USER_INFO_NOT_EXIST);
        }
        UserCoupon userCoupon = userCouponService.claimDailyCoupon(loginUser.getId());
        return Result.OK(userCoupon);
    }

}