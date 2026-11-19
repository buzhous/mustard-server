package org.jeecg.modules.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.app.entity.RechargeOrder;


public interface RechargeOrderMapper extends BaseMapper<RechargeOrder> {

    /**
     * 乐观锁定单成功：仅当当前状态为待支付时更新，防止重复回调重复加积分
     */
    @Update("UPDATE tbl_recharge_order SET status = #{status}, pay_time = NOW(), update_time = NOW() WHERE order_no = #{orderNo} AND status = #{expectStatus}")
    int casUpdateStatus(@Param("orderNo") String orderNo, @Param("expectStatus") Integer expectStatus, @Param("status") Integer status);

}
