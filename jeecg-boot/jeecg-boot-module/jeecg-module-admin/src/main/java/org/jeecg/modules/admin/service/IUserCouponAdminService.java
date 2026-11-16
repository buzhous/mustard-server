package org.jeecg.modules.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.admin.entity.coupon.UserCouponAdmin;

import java.util.List;

/**
 * 用户持券 后台管理服务
 */
public interface IUserCouponAdminService extends IService<UserCouponAdmin> {

    /**
     * 系统发放指定券给用户（obtainType=2，原子扣减库存防超发）
     *
     * @param userId   用户Id
     * @param couponId 券Id
     * @param count    发放张数
     */
    void grantCoupon(String userId, String couponId, int count);

    /**
     * 批量作废未使用的持券记录（status=0 → 3），并按券回补库存
     *
     * @param ids 持券记录Id列表
     * @return 实际作废张数
     */
    int invalidateByIds(List<String> ids);
}
