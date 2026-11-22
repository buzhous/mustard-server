package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.bean.vo.item.ItemInfoCreateVO;
import org.jeecg.modules.app.bean.vo.item.ItemInfoQueryVO;
import org.jeecg.modules.app.entity.ItemInfo;


public interface IItemInfoService extends IService<ItemInfo> {

    /**
     * 查询物品信息
     *
     * @param itemId 物品ID
     * @return 物品信息
     */
    ItemInfo queryItemInfo(String itemId);

    /**
     * 查找物品Id
     *
     * @param userId 用户ID
     * @param oriId 原始物品ID
     * @return 物品信息
     */
    ItemInfo queryItemInfoByOriId(String userId, String oriId);

    /**
     * 创建物品信息
     *
     * @param createVO 创建VO
     * @return 是否创建成功
     */
    boolean createItemInfo(ItemInfoCreateVO createVO);

    /**
     * 创建物品信息（指定账本 operator：开放平台 Agent 写入传 LineageService.operatorAgent(appKey)）
     *
     * @param createVO 创建VO
     * @param operator 账本操作者标识（user / agent:xxxx）
     * @return 是否创建成功
     */
    boolean createItemInfo(ItemInfoCreateVO createVO, String operator);

    /**
     * 创建物品信息（临时）
     *
     * @param itemInfoCreateVO 创建VO
     * @return 是否创建成功
     */
    boolean createItemInfoTmp(ItemInfoCreateVO itemInfoCreateVO);

    boolean updateItemInfo(ItemInfoCreateVO createVO);

    /**
     * 条件分页查询物品（关键字/仓位/状态/排序，供 App 端与开放平台共用）
     *
     * @param queryVO 查询条件
     * @return 分页结果
     */
    Page<ItemInfo> listByCondition(ItemInfoQueryVO queryVO);

}
