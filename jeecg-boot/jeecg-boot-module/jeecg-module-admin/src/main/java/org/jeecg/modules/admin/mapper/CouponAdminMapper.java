package org.jeecg.modules.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.admin.entity.coupon.CouponAdmin;

/**
 * @Description: tbl_coupon 后台管理
 */
public interface CouponAdminMapper extends BaseMapper<CouponAdmin> {

    /**
     * 原子扣减库存（发放券防超发），affected rows = 0 表示库存不足
     */
    @Update("UPDATE tbl_coupon SET remain_count = remain_count - #{count}, update_time = NOW() "
            + "WHERE id = #{id} AND remain_count >= #{count}")
    int deductStock(@Param("id") String id, @Param("count") int count);

    /**
     * 回补库存（作废未使用券）
     */
    @Update("UPDATE tbl_coupon SET remain_count = remain_count + #{count}, update_time = NOW() WHERE id = #{id}")
    int restoreStock(@Param("id") String id, @Param("count") int count);

    /**
     * 库存增减（总量与剩余同步调整），affected rows = 0 表示剩余数量不足
     */
    @Update("UPDATE tbl_coupon SET remain_count = remain_count + #{delta}, total_count = total_count + #{delta}, update_time = NOW() "
            + "WHERE id = #{id} AND remain_count + #{delta} >= 0")
    int adjustStock(@Param("id") String id, @Param("delta") int delta);
}
