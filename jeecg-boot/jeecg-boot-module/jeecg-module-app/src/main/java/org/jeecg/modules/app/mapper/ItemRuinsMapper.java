package org.jeecg.modules.app.mapper;

import org.jeecg.modules.app.entity.ItemRuins;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

/**
 * @Description: tbl_item_ruins
 * @Author: jeecg-boot
 * @Date:   2026-04-28
 * @Version: V1.0
 */
public interface ItemRuinsMapper extends BaseMapper<ItemRuins> {

    /**
     * 原子翻转殷墟行：仅未拾取(status=0)可更新，影响行数为0表示已被别人拾取；
     * 若已被管理员指派给其他用户（pickup_user 非空且不等于当前用户）则同样翻转失败
     *
     * @param id         殷墟记录ID
     * @param pickupUser 拾取者用户ID
     * @param pickupTime 拾取时间
     * @return 影响行数
     */
    @Update("UPDATE tbl_item_ruins SET status = 1, pickup_user = #{pickupUser}, pickup_time = #{pickupTime}, "
            + "update_time = #{pickupTime} WHERE id = #{id} AND status = 0 "
            + "AND (pickup_user IS NULL OR pickup_user = '' OR pickup_user = #{pickupUser})")
    int pickupRuins(@Param("id") Long id, @Param("pickupUser") String pickupUser, @Param("pickupTime") Date pickupTime);

    /**
     * 拾取成功后更新殷墟记录关联的新物品ID（仅已拾取(status=1)可更新）
     *
     * @param id     殷墟记录ID
     * @param itemId 新物品ID
     * @return 影响行数
     */
    @Update("UPDATE tbl_item_ruins SET item_id = #{itemId}, update_time = NOW() WHERE id = #{id} AND status = 1")
    int updateRuinsItemId(@Param("id") Long id, @Param("itemId") String itemId);

}
