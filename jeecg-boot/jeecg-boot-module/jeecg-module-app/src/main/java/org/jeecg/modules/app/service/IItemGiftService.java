package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.bean.vo.gift.ItemGiftListVO;
import org.jeecg.modules.app.bean.vo.gift.ItemGiftReceiveVO;
import org.jeecg.modules.app.bean.vo.gift.ItemGiftReqVO;
import org.jeecg.modules.app.bean.vo.gift.ItemGiftRspVO;
import org.jeecg.modules.app.bean.vo.gift.ReceiveGiftReqVO;
import org.jeecg.modules.app.entity.ItemGift;

import java.util.List;
import java.util.Map;

/**
 * 物品赠送服务接口
 */
public interface IItemGiftService extends IService<ItemGift> {

    /**
     * 创建赠送记录
     *
     * @param reqVO    赠送请求
     * @param userId   赠送者用户ID
     * @return 赠送响应
     */
    ItemGiftRspVO createGift(ItemGiftReqVO reqVO, String userId);

    /**
     * 创建赠送记录（指定账本 operator：开放平台 Agent 赠送传 LineageService.operatorAgent(appKey)）
     *
     * @param reqVO    赠送请求
     * @param userId   赠送者用户ID
     * @param operator 账本操作者标识（user / agent:xxxx）
     * @return 赠送响应
     */
    ItemGiftRspVO createGift(ItemGiftReqVO reqVO, String userId, String operator);

    /**
     * 领取赠送物品
     *
     * @param reqVO   领取请求
     * @param userId  领取者用户ID
     * @return shipRecordId/shipDepositNo：自建仓实物领取后自动创建的发运单（其余场景为 null）
     */
    Map<String, Object> receiveGift(ReceiveGiftReqVO reqVO, String userId);

    /**
     * 撤回赠送
     *
     * @param giftId 赠送记录ID
     * @param userId 操作人用户ID
     * @return 是否撤回成功
     */
    boolean cancelGift(String giftId, String userId);

    /**
     * 查询用户赠送记录列表
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @param type   类型：1-我赠送的，2-我收到的
     * @return 分页结果
     */
    Page<ItemGiftListVO> queryGiftList(Page<ItemGift> page, String userId, Integer type);

    /**
     * 查询赠送领取明细（多人领取记录）
     *
     * @param giftId 赠送记录ID
     * @return 领取明细列表（昵称、头像、数量、领取时间）
     */
    List<ItemGiftReceiveVO> queryGiftReceivers(String giftId);

    /**
     * 查询赠送详情（物品 + 双方昵称头像 + 领取记录 + 实物发运单）
     *
     * @param giftId       赠送记录ID
     * @param currentUserId 当前用户ID（仅寄件人/受赠人可见，否则抛 GIFT_NOT_EXIST）
     * @return 赠送详情VO
     */
    ItemGiftListVO queryGiftDetail(String giftId, String currentUserId);

    /**
     * 根据兑换码查询赠送记录
     *
     * @param giftCode 兑换码
     * @return 赠送记录
     */
    ItemGift getGiftByCode(String giftCode);

}