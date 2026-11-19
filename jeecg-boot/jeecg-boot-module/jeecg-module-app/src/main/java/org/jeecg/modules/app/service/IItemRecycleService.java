package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.bean.vo.ruins.ItemRuinsListVO;
import org.jeecg.modules.app.entity.ItemRecycle;
import org.jeecg.modules.app.entity.ItemSync;

/**
 * 用户独立回收站：删除的物品进入本人回收站（与殷墟/公海隔离），保留 30 天，
 * 期间可恢复（撤回删除）或彻底删除，超期由定时任务清理
 */
public interface IItemRecycleService extends IService<ItemRecycle> {

    /**
     * 删除物品落入本人回收站（每条删除独立成记录，可单独恢复）
     */
    boolean dropToRecycle(ItemSync itemSync);

    /**
     * 回收站分页列表：本人 30 天内删除的记录，按删除时间倒序
     */
    IPage<ItemRuinsListVO> queryMyRecyclePage(Integer page, Integer size, String userId);

    /**
     * 回收站恢复：撤回删除，物品回库存，回收站记录标记已恢复
     */
    boolean restoreFromRecycle(String userId, String recycleId, String itemId);

    /**
     * 回收站彻底删除：回收站记录物理删除，全量删除的同步记录一并删除
     */
    boolean purgeRecycle(String userId, String recycleId, String itemId);

    /**
     * 清理超期回收站：删除 30 天前未恢复的记录（定时任务调用），返回清理条数
     */
    int purgeExpiredRecycle();

}
