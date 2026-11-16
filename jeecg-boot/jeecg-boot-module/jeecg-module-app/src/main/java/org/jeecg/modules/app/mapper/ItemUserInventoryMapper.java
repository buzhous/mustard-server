package org.jeecg.modules.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.app.entity.ItemUserInventory;


public interface ItemUserInventoryMapper extends BaseMapper<ItemUserInventory> {

    /**
     * 条件原子扣减库存数量：quantity = quantity - ? WHERE id = ? AND quantity >= ?
     *
     * @param id       库存ID
     * @param quantity 扣减数量
     * @return 影响行数（0表示数量不足）
     */
    @Update("UPDATE tbl_item_user_inventory SET quantity = quantity - #{quantity}, update_time = NOW() "
            + "WHERE id = #{id} AND quantity >= #{quantity}")
    int decreaseQuantityIfEnough(@Param("id") String id, @Param("quantity") Integer quantity);

    /**
     * 原子累加库存数量
     *
     * @param id       库存ID
     * @param quantity 累加数量
     * @return 影响行数
     */
    @Update("UPDATE tbl_item_user_inventory SET quantity = quantity + #{quantity}, update_time = NOW() "
            + "WHERE id = #{id}")
    int increaseQuantity(@Param("id") String id, @Param("quantity") Integer quantity);

}
