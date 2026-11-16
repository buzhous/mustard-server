package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.bean.vo.ruins.ItemRuinsListVO;
import org.jeecg.modules.app.entity.ItemRuins;
import org.jeecg.modules.app.entity.ItemSync;


public interface IItemRuinsService extends IService<ItemRuins> {

    IPage<ItemRuinsListVO> queryNotPickupRuinsPage(Integer pageNo, Integer pageSize, String userId);

    ItemRuins queryRuinByItemId(String itemId, String oriId);

    ItemRuins queryRuinByRuinsId(String ruinsId, String itemId);

    boolean dropToRuins(ItemSync itemSync);

    /**
     * 拾取殷墟物品：事务内先锁定一张可用殷墟拾取券，再原子翻转殷墟行，
     * 同步物品到拾取者名下后标记券已使用
     *
     * @param runsId 殷墟记录ID
     * @param itemId 殷墟物品ID
     * @param userId 拾取者用户ID
     * @return 拾取者名下的物品同步记录
     */
    ItemSync pickup(String runsId, String itemId, String userId);

}
