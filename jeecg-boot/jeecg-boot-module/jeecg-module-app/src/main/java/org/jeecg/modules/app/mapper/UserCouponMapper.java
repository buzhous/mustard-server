package org.jeecg.modules.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.app.entity.UserCoupon;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 原子锁定一张可用指定编码的优惠券（FOR UPDATE 行锁，需在事务内调用）
     *
     * @param userId 用户ID
     * @param code   优惠券编码
     * @return 锁定的用户优惠券，无可用券返回null
     */
    @Select("SELECT uc.* FROM tbl_user_coupon uc INNER JOIN tbl_coupon c ON uc.coupon_id = c.id "
            + "WHERE uc.user_id = #{userId} AND uc.status = 0 AND c.code = #{code} "
            + "AND uc.valid_start_time <= NOW() AND uc.valid_end_time >= NOW() "
            + "ORDER BY uc.id ASC LIMIT 1 FOR UPDATE")
    UserCoupon lockAvailableCouponByCode(@Param("userId") String userId, @Param("code") String code);

    /**
     * 统计用户可用指定编码的优惠券数量（未使用且未过期）
     *
     * @param userId 用户ID
     * @param code   优惠券编码
     * @return 可用数量
     */
    @Select("SELECT COUNT(*) FROM tbl_user_coupon uc INNER JOIN tbl_coupon c ON uc.coupon_id = c.id "
            + "WHERE uc.user_id = #{userId} AND uc.status = 0 AND c.code = #{code} "
            + "AND uc.valid_start_time <= NOW() AND uc.valid_end_time >= NOW()")
    int countAvailableByCode(@Param("userId") String userId, @Param("code") String code);

}
