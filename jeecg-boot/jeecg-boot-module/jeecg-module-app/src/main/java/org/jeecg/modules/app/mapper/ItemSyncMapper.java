package org.jeecg.modules.app.mapper;

import org.jeecg.modules.app.entity.ItemSync;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * @Description: 物品同步
 * @Author: jeecg-boot
 * @Date:   2026-03-26
 * @Version: V1.0
 */
public interface ItemSyncMapper extends BaseMapper<ItemSync> {

    /**
     * 条件原子标记销毁：仅未销毁状态可翻转，防止重复销毁并发覆盖
     *
     * @param id      同步记录ID
     * @param status  目标状态（SyncStatusEnum.DESTROYED）
     * @param version 新版本号
     * @return 影响行数（0表示已销毁）
     */
    @Update("UPDATE tbl_item_sync SET status = #{status}, version = #{version}, update_time = NOW() "
            + "WHERE id = #{id} AND (status IS NULL OR status <> #{status})")
    int markDestroyedIfNot(@Param("id") Long id, @Param("status") Integer status, @Param("version") Integer version);

}
