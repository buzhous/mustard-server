package org.jeecg.modules.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.app.entity.Coupon;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 根据编码查询优惠券
     *
     * @param code 优惠券编码
     * @return 优惠券，不存在返回null
     */
    @Select("SELECT * FROM tbl_coupon WHERE code = #{code} LIMIT 1")
    Coupon selectByCode(@Param("code") String code);

}
