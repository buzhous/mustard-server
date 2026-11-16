package org.jeecg.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.admin.constant.CouponAdminConstant;
import org.jeecg.modules.admin.entity.coupon.CouponAdmin;
import org.jeecg.modules.admin.entity.coupon.UserCouponAdmin;
import org.jeecg.modules.admin.mapper.CouponAdminMapper;
import org.jeecg.modules.admin.mapper.UserCouponAdminMapper;
import org.jeecg.modules.admin.service.IUserCouponAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户持券 后台管理服务实现
 */
@Service
public class UserCouponAdminServiceImpl extends ServiceImpl<UserCouponAdminMapper, UserCouponAdmin> implements IUserCouponAdminService {

    @Autowired
    private CouponAdminMapper couponAdminMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantCoupon(String userId, String couponId, int count) {
        // 原子扣减库存防超发（affected rows = 0 表示库存不足）
        int rows = couponAdminMapper.deductStock(couponId, count);
        if (rows == 0) {
            throw new JeecgBootException("券剩余库存不足，发放失败");
        }
        CouponAdmin coupon = couponAdminMapper.selectById(couponId);
        Date now = new Date();
        List<UserCouponAdmin> records = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UserCouponAdmin uc = new UserCouponAdmin();
            uc.setUserId(userId);
            uc.setCouponId(coupon.getId());
            uc.setCouponName(coupon.getCouponName());
            uc.setCouponType(coupon.getCouponType());
            uc.setDiscountValue(coupon.getDiscountValue());
            uc.setMinAmount(coupon.getMinAmount());
            uc.setValidStartTime(coupon.getValidStartTime());
            uc.setValidEndTime(coupon.getValidEndTime());
            uc.setObtainType(CouponAdminConstant.OBTAIN_TYPE_GRANT);
            uc.setStatus(CouponAdminConstant.UC_STATUS_UNUSED);
            uc.setCreateTime(now);
            uc.setUpdateTime(now);
            records.add(uc);
        }
        this.saveBatch(records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int invalidateByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<UserCouponAdmin> toInvalidate = this.list(new LambdaQueryWrapper<UserCouponAdmin>()
                .in(UserCouponAdmin::getId, ids)
                .eq(UserCouponAdmin::getStatus, CouponAdminConstant.UC_STATUS_UNUSED));
        if (toInvalidate.isEmpty()) {
            return 0;
        }
        // 条件更新防并发（仅未使用状态可作废）
        int rows = this.baseMapper.update(null, new LambdaUpdateWrapper<UserCouponAdmin>()
                .in(UserCouponAdmin::getId, ids)
                .eq(UserCouponAdmin::getStatus, CouponAdminConstant.UC_STATUS_UNUSED)
                .set(UserCouponAdmin::getStatus, CouponAdminConstant.UC_STATUS_INVALID)
                .set(UserCouponAdmin::getUpdateTime, new Date()));
        if (rows > 0) {
            // 作废回补库存：按券分组回补
            Map<String, Long> restoreMap = toInvalidate.stream()
                    .collect(Collectors.groupingBy(UserCouponAdmin::getCouponId, Collectors.counting()));
            restoreMap.forEach((couponId, cnt) -> couponAdminMapper.restoreStock(couponId, cnt.intValue()));
        }
        return rows;
    }
}
