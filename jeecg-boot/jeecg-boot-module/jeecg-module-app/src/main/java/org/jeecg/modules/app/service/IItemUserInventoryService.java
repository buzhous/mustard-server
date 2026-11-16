package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.bean.vo.inventory.ItemInventoryVO;
import org.jeecg.modules.app.entity.ItemUserInventory;

import java.util.List;


public interface IItemUserInventoryService extends IService<ItemUserInventory> {

    ItemUserInventory queryUserItem(String userId, String itemId);

    /**
     * 创建用户物品库存
     *
     * @param itemInventoryVO 用户物品库存VO
     * @return 是否创建成功
     */
    boolean createItemUserInventory(ItemInventoryVO itemInventoryVO);

    /**
     * 创建临时用户物品库存
     *
     * @param itemInventoryVO 用户物品库存VO
     * @return 是否创建成功
     */
    boolean createItemUserInventoryTmp(ItemInventoryVO itemInventoryVO);

    /**
     * 更新用户物品库存
     *
     * @param inventoryVO 用户物品库存VO
     * @return 是否更新成功
     */
    boolean updateItemUserInventory(ItemInventoryVO inventoryVO);


    List<ItemUserInventory> queryItemUserInventoryList(String userId, Integer isSync);

    boolean checkExistItem(String userId);

    // 检测用户是否有线上物品
    boolean updateItemSync(ItemUserInventory inventory);

    boolean updateItemById(ItemUserInventory inventory);

    List<ItemInventoryVO> queryUserItems(String userId);

    boolean removeUserItem(String userId, String inventoryId);

    /**
     * 条件原子扣减库存数量（quantity = quantity - ? WHERE id = ? AND quantity >= ?）
     *
     * @param id       库存ID
     * @param quantity 扣减数量
     * @return 影响行数（0表示数量不足）
     */
    int decreaseQuantityIfEnough(String id, Integer quantity);

    /**
     * 原子累加库存数量
     *
     * @param id       库存ID
     * @param quantity 累加数量
     * @return 影响行数
     */
    int increaseQuantity(String id, Integer quantity);

}
