package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.entity.UserCoupon;

import java.util.List;

public interface IUserCouponService extends IService<UserCoupon> {

    Page<UserCoupon> queryUserCouponPage(Page<UserCoupon> page, String userId, Integer status);

    List<UserCoupon> queryAvailableCouponsByUserId(String userId);

    boolean receiveCoupon(String userId, Long couponId);

    boolean useCoupon(String userId, Long userCouponId, String orderId);

    int countUserReceivedCoupons(String userId, Long couponId);

    int countUserDailyReceivedCoupons(String userId, Long couponId);

    int countAvailableCouponsByUserId(String userId);

    UserCoupon getByUserIdAndCouponId(String userId, Long couponId, String orderId);

    /**
     * 原子锁定一张可用的殷墟拾取券（FOR UPDATE 行锁，需在事务内调用）
     *
     * @param userId 用户ID
     * @return 锁定的用户优惠券，无可用券返回null
     */
    UserCoupon lockAvailableRuinsPickupCoupon(String userId);

    /**
     * 统计用户可用殷墟拾取券数量（未使用且未过期）
     *
     * @param userId 用户ID
     * @return 可用数量
     */
    int countAvailableRuinsPickupCoupons(String userId);

    /**
     * 每日领取一张殷墟拾取券（每人每日限领1张，券不存在时惰性创建）
     *
     * @param userId 用户ID
     * @return 领取到的用户优惠券
     */
    UserCoupon claimDailyCoupon(String userId);

}