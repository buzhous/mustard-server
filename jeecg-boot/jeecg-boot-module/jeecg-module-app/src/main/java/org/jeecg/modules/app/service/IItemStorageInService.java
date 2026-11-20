package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.bean.vo.storagein.MoveReceiverConfirmReqVO;
import org.jeecg.modules.app.bean.vo.storagein.StorageInDetailVO;
import org.jeecg.modules.app.bean.vo.storagein.StorageInListVO;
import org.jeecg.modules.app.bean.vo.storagein.StorageAuthSubmitReqVO;
import org.jeecg.modules.app.bean.vo.storagein.TransferInReqVO;
import org.jeecg.modules.app.entity.ItemStorageIn;
import org.jeecg.modules.app.entity.KuaidiOrder;
import org.jeecg.modules.app.entity.warehouse.UserStorage;

/**
 * 物品转入/移库 + 自建仓认证服务
 *
 * @author elinx
 */
public interface IItemStorageInService extends IService<ItemStorageIn> {

    // ---- 自建仓认证 ----

    /** 提交平台认证（材料：营业执照/门头照/联系人/电话） */
    void submitAuth(String userId, StorageAuthSubmitReqVO reqVO);

    /** 查询认证状态（含驳回原因），仅限仓主 */
    UserStorage queryAuthStatus(String userId, String storageId);

    // ---- 转入 ----

    /** 物品转入：targetType=3 自建仓(需已认证,待确认转移) / =2 官方仓(待邮寄) */
    ItemStorageIn transferIn(String userId, TransferInReqVO reqVO);

    /** 仓主确认已收到（自建仓 10→20，物品落自建仓） */
    ItemStorageIn confirmSelf(String userId, String recordId);

    /** 回填快递单号（官方仓 15→16） */
    ItemStorageIn expressBack(String userId, String recordId, String expressNo);

    /** 取消单据（10/15/16/30 可取消，解锁物品） */
    ItemStorageIn cancel(String userId, String recordId);

    // ---- 发运（赠送领取后快递寄送） ----

    /** 接收方确认落仓信息并触发快递100下单（30→31） */
    ItemStorageIn moveReceiverConfirm(String userId, MoveReceiverConfirmReqVO reqVO);

    /** 寄件方标记线下当面交付（30→40，待接收方确认收货） */
    ItemStorageIn moveOfflineDeliver(String userId, String recordId);

    /** 接收方确认线下收货并选择入库仓（40→33，实体落所选仓） */
    ItemStorageIn moveOfflineConfirm(String userId, MoveReceiverConfirmReqVO reqVO);

    /** 接收方手动确认签收（31/32→33，与快递回调签收同一入库逻辑） */
    ItemStorageIn receiverSign(String userId, String recordId);

    // ---- 查询 ----

    /**
     * 单据列表
     * queryType=1 我发起的；queryType=2 待我接收/我接收的移库单
     * bizType 空=全部，1 转入单，2 移库单
     */
    Page<StorageInListVO> queryList(Page<ItemStorageIn> page, String userId, Integer queryType, Integer bizType);

    /** 单据详情（含官方仓地址/快递收寄双方信息） */
    StorageInDetailVO queryDetail(String userId, String recordId);

    // ---- 官方后台（jeecg 后台 JWT） ----

    /** 后台分页（按单号/状态/业务类型筛选） */
    Page<StorageInListVO> adminPage(Integer pageNo, Integer pageSize, String depositNo, Integer status, Integer bizType);

    /** 后台确认官方入库（16→25，物品落官方仓） */
    ItemStorageIn adminConfirmOfficial(String recordId);

    /** 后台单据详情（不校验归属） */
    StorageInDetailVO adminDetail(String recordId);

    // ---- 快递联动 ----

    /** 快递状态联动：揽收/运输 → 32；签收 → 33 + 归属转移 */
    void onKuaidiStatus(KuaidiOrder order, int newStatus);

    // ---- 校验 ----

    /** 物品是否存在在途/待确认单（锁定判定） */
    boolean isItemLocked(String itemId);
}
