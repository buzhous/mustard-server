package org.jeecg.modules.app.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.boot.starter.lock.annotation.JLock;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.constant.CouponConstant;
import org.jeecg.modules.app.constant.LockConstant;
import org.jeecg.modules.app.entity.Coupon;
import org.jeecg.modules.app.entity.UserCoupon;
import org.jeecg.modules.app.mapper.UserCouponMapper;
import org.jeecg.modules.app.service.ICouponService;
import org.jeecg.modules.app.service.IUserCouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {

    @Autowired
    private ICouponService couponService;

    @Override
    public Page<UserCoupon> queryUserCouponPage(Page<UserCoupon> page, String userId, Integer status) {
        LambdaQueryWrapper<UserCoupon> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCoupon::getUserId, userId);
        if (ObjectUtil.isNotEmpty(status)) {
            queryWrapper.eq(UserCoupon::getStatus, status);
        }
        queryWrapper.orderByDesc(UserCoupon::getCreateTime);
        return this.page(page, queryWrapper);
    }

    @Override
    public List<UserCoupon> queryAvailableCouponsByUserId(String userId) {
        LambdaQueryWrapper<UserCoupon> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCoupon::getUserId, userId);
        queryWrapper.eq(UserCoupon::getStatus, 0);
        queryWrapper.le(UserCoupon::getValidStartTime, new Date());
        queryWrapper.ge(UserCoupon::getValidEndTime, new Date());
        queryWrapper.orderByDesc(UserCoupon::getCreateTime);
        return this.list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // lockKey 含 # 走 SpEL 解析，字面量必须加单引号，否则冒号导致 EL1041E 解析失败
    @JLock(lockKey = "'" + LockConstant.COUPON_RECEIVE_LOCK + "' + #couponId")
    public boolean receiveCoupon(String userId, Long couponId) {
        Coupon coupon = couponService.getById(couponId);
        if (ObjectUtil.isEmpty(coupon)) {
            throw new AppException(ExceptionEnum.COUPON_NOT_EXIST);
        }
        if (coupon.getStatus() != 1) {
            throw new AppException(ExceptionEnum.COUPON_NOT_AVAILABLE);
        }
        if (coupon.getRemainCount() <= 0) {
            throw new AppException(ExceptionEnum.COUPON_STOCK_EMPTY);
        }
        Date now = new Date();
        if (now.before(coupon.getValidStartTime()) || now.after(coupon.getValidEndTime())) {
            throw new AppException(ExceptionEnum.COUPON_NOT_IN_VALID_TIME);
        }
        int receivedCount = this.countUserReceivedCoupons(userId, couponId);
        if (coupon.getPerLimit() > 0 && receivedCount >= coupon.getPerLimit()) {
            throw new AppException(ExceptionEnum.COUPON_RECEIVE_LIMIT_EXCEEDED);
        }
        if (coupon.getDailyLimit() != null && coupon.getDailyLimit() > 0) {
            int dailyReceivedCount = this.countUserDailyReceivedCoupons(userId, couponId);
            if (dailyReceivedCount >= coupon.getDailyLimit()) {
                throw new AppException(ExceptionEnum.COUPON_DAILY_LIMIT_EXCEEDED);
            }
        }
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setCouponName(coupon.getCouponName());
        userCoupon.setCouponType(coupon.getCouponType());
        userCoupon.setDiscountValue(coupon.getDiscountValue());
        userCoupon.setMinAmount(coupon.getMinAmount());
        userCoupon.setValidStartTime(coupon.getValidStartTime());
        userCoupon.setValidEndTime(coupon.getValidEndTime());
        userCoupon.setObtainType(1);
        userCoupon.setStatus(0);
        userCoupon.setCreateTime(DateUtil.date());
        userCoupon.setUpdateTime(DateUtil.date());
        boolean saved = this.save(userCoupon);
        if (saved) {
            coupon.setRemainCount(coupon.getRemainCount() - 1);
            coupon.setUpdateTime(DateUtil.date());
            couponService.updateById(coupon);
        }
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean useCoupon(String userId, Long userCouponId, String orderId) {
        UserCoupon userCoupon = this.getById(userCouponId);
        if (ObjectUtil.isEmpty(userCoupon)) {
            throw new AppException(ExceptionEnum.USER_COUPON_NOT_EXIST);
        }
        if (!userCoupon.getUserId().equals(userId)) {
            throw new AppException(ExceptionEnum.COUPON_NOT_BELONG_TO_USER);
        }
        if (userCoupon.getStatus() != 0) {
            throw new AppException(ExceptionEnum.COUPON_ALREADY_USED);
        }
        Date now = new Date();
        if (now.after(userCoupon.getValidEndTime())) {
            throw new AppException(ExceptionEnum.COUPON_EXPIRED);
        }
        userCoupon.setStatus(1);
        userCoupon.setUseTime(now);
        userCoupon.setOrderId(orderId);
        userCoupon.setUpdateTime(now);
        return this.updateById(userCoupon);
    }

    @Override
    public int countUserReceivedCoupons(String userId, Long couponId) {
        LambdaQueryWrapper<UserCoupon> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCoupon::getUserId, userId);
        queryWrapper.eq(UserCoupon::getCouponId, couponId);
        return (int) this.count(queryWrapper);
    }

    @Override
    public int countUserDailyReceivedCoupons(String userId, Long couponId) {
        LambdaQueryWrapper<UserCoupon> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCoupon::getUserId, userId);
        queryWrapper.eq(UserCoupon::getCouponId, couponId);
        Date today = DateUtil.beginOfDay(new Date());
        Date tomorrow = DateUtil.offsetDay(today, 1);
        queryWrapper.ge(UserCoupon::getCreateTime, today);
        queryWrapper.lt(UserCoupon::getCreateTime, tomorrow);
        return (int) this.count(queryWrapper);
    }

    @Override
    public int countAvailableCouponsByUserId(String userId) {
        LambdaQueryWrapper<UserCoupon> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCoupon::getUserId, userId);
        queryWrapper.eq(UserCoupon::getStatus, 0);
        queryWrapper.le(UserCoupon::getValidStartTime, new Date());
        queryWrapper.ge(UserCoupon::getValidEndTime, new Date());
        return (int) this.count(queryWrapper);
    }

    @Override
    public UserCoupon getByUserIdAndCouponId(String userId, Long couponId, String orderId) {
        LambdaQueryWrapper<UserCoupon> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCoupon::getUserId, userId);
        queryWrapper.eq(UserCoupon::getCouponId, couponId);
        queryWrapper.eq(UserCoupon::getStatus, 0);
        if (ObjectUtil.isNotEmpty(orderId)) {
            queryWrapper.eq(UserCoupon::getOrderId, orderId);
        }
        queryWrapper.last("limit 1");
        return this.getOne(queryWrapper);
    }

    @Override
    public UserCoupon lockAvailableRuinsPickupCoupon(String userId) {
        return this.baseMapper.lockAvailableCouponByCode(userId, CouponConstant.RUINS_PICKUP_CODE);
    }

    @Override
    public int countAvailableRuinsPickupCoupons(String userId) {
        return this.baseMapper.countAvailableByCode(userId, CouponConstant.RUINS_PICKUP_CODE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @JLock(lockKey = "'" + LockConstant.COUPON_CLAIM_DAILY_LOCK + "' + #userId")
    public UserCoupon claimDailyCoupon(String userId) {
        Coupon coupon = getOrCreateRuinsPickupCoupon();
        // 每日限领数量读取券配置（后台可调，未配置默认1张）：按 user_id + 当日已领判定
        int dailyLimit = CouponConstant.resolveDailyClaimLimit(coupon.getDailyLimit());
        int dailyReceivedCount = this.countUserDailyReceivedCoupons(userId, coupon.getId());
        if (dailyReceivedCount >= dailyLimit) {
            throw new AppException(ExceptionEnum.COUPON_DAILY_ALREADY_CLAIMED);
        }
        Date now = DateUtil.date();
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(coupon.getId());
        userCoupon.setCouponName(coupon.getCouponName());
        userCoupon.setCouponType(coupon.getCouponType());
        userCoupon.setDiscountValue(coupon.getDiscountValue());
        userCoupon.setMinAmount(coupon.getMinAmount());
        userCoupon.setValidStartTime(coupon.getValidStartTime());
        userCoupon.setValidEndTime(coupon.getValidEndTime());
        userCoupon.setObtainType(1);
        userCoupon.setStatus(0);
        userCoupon.setCreateTime(now);
        userCoupon.setUpdateTime(now);
        this.save(userCoupon);
        return userCoupon;
    }

    /**
     * 按固定编码获取殷墟拾取券，不存在时惰性创建（库存极大值、长期有效、已发布）
     */
    private Coupon getOrCreateRuinsPickupCoupon() {
        Coupon coupon = couponService.getByCode(CouponConstant.RUINS_PICKUP_CODE);
        if (ObjectUtil.isNotEmpty(coupon)) {
            return coupon;
        }
        Date now = DateUtil.date();
        Coupon newCoupon = new Coupon();
        newCoupon.setCode(CouponConstant.RUINS_PICKUP_CODE);
        newCoupon.setCouponName(CouponConstant.RUINS_PICKUP_NAME);
        newCoupon.setCouponType(CouponConstant.COUPON_TYPE_EXCHANGE);
        newCoupon.setDescription("每日可领取1张，用于废墟广场拾取物品，拾取一次消耗一张");
        newCoupon.setValidStartTime(now);
        newCoupon.setValidEndTime(DateUtil.parse("2099-12-31 23:59:59"));
        newCoupon.setTotalCount(CouponConstant.RUINS_PICKUP_TOTAL_COUNT);
        newCoupon.setRemainCount(CouponConstant.RUINS_PICKUP_TOTAL_COUNT);
        newCoupon.setUsedCount(0);
        newCoupon.setPerLimit(0);
        newCoupon.setDailyLimit(1);
        newCoupon.setStatus(CouponConstant.COUPON_STATUS_PUBLISHED);
        newCoupon.setSort(0);
        newCoupon.setCreateTime(now);
        newCoupon.setUpdateTime(now);
        try {
            couponService.save(newCoupon);
            return newCoupon;
        } catch (DuplicateKeyException e) {
            // 并发惰性创建时唯一索引兜底，回查已有记录
            Coupon existCoupon = couponService.getByCode(CouponConstant.RUINS_PICKUP_CODE);
            if (ObjectUtil.isEmpty(existCoupon)) {
                throw new AppException(ExceptionEnum.COUPON_NOT_EXIST);
            }
            return existCoupon;
        }
    }

}