package org.jeecg.modules.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.app.entity.UserAmount;


public interface UserAmountMapper extends BaseMapper<UserAmount> {

    /**
     * 原子增加金币（积分），返回影响行数
     */
    @Update("UPDATE tbl_user_amount SET gold_coin_amount = gold_coin_amount + #{points}, update_time = NOW() WHERE user_id = #{userId}")
    int addGoldCoin(@Param("userId") String userId, @Param("points") Integer points);

}
