package org.jeecg.modules.app.controller.storagein;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.vo.storagein.MoveReceiverConfirmReqVO;
import org.jeecg.modules.app.bean.vo.storagein.StorageInDetailVO;
import org.jeecg.modules.app.bean.vo.storagein.StorageInListVO;
import org.jeecg.modules.app.bean.vo.storagein.StorageAuthSubmitReqVO;
import org.jeecg.modules.app.bean.vo.storagein.TransferInReqVO;
import org.jeecg.modules.app.entity.ItemStorageIn;
import org.jeecg.modules.app.entity.warehouse.UserStorage;
import org.jeecg.modules.app.service.IItemStorageInService;
import org.jeecg.modules.app.utils.AppAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 物品转入/移库 + 自建仓认证
 *
 * @author elinx
 */
@Slf4j
@Tag(name = "物品转入/移库")
@RestController
@RequestMapping("/app/storage")
public class AppStorageInController {

    @Autowired
    private IItemStorageInService itemStorageInService;

    // ==================== 自建仓认证 ====================

    @Operation(summary = "提交平台认证")
    @PostMapping("/auth/submit")
    public Result<Void> submitAuth(@RequestBody StorageAuthSubmitReqVO reqVO) {
        itemStorageInService.submitAuth(AppAuthUtil.getUserInfo().getId(), reqVO);
        return Result.OK("认证材料已提交，等待审核");
    }

    @Operation(summary = "查询认证状态")
    @GetMapping("/auth/status")
    public Result<UserStorage> authStatus(@RequestParam String storageId) {
        return Result.OK(itemStorageInService.queryAuthStatus(AppAuthUtil.getUserInfo().getId(), storageId));
    }

    // ==================== 转入 ====================

    @Operation(summary = "物品转入（自建仓待确认/官方仓待邮寄）")
    @PostMapping("/transferIn")
    public Result<ItemStorageIn> transferIn(@RequestBody TransferInReqVO reqVO) {
        return Result.OK(itemStorageInService.transferIn(AppAuthUtil.getUserInfo().getId(), reqVO));
    }

    @Operation(summary = "仓主确认已收到（自建仓）")
    @PostMapping("/transferIn/confirmSelf")
    public Result<ItemStorageIn> confirmSelf(@RequestParam String recordId) {
        return Result.OK(itemStorageInService.confirmSelf(AppAuthUtil.getUserInfo().getId(), recordId));
    }

    @Operation(summary = "回填快递单号（官方仓）")
    @PostMapping("/transferIn/express")
    public Result<ItemStorageIn> expressBack(@RequestParam String recordId, @RequestParam String expressNo) {
        return Result.OK(itemStorageInService.expressBack(AppAuthUtil.getUserInfo().getId(), recordId, expressNo));
    }

    @Operation(summary = "取消转入/移库单")
    @PostMapping("/transferIn/cancel")
    public Result<ItemStorageIn> cancel(@RequestParam String recordId) {
        return Result.OK(itemStorageInService.cancel(AppAuthUtil.getUserInfo().getId(), recordId));
    }

    @Operation(summary = "我的转入/寄放单列表")
    @GetMapping("/transferIn/list")
    public Result<Page<StorageInListVO>> transferInList(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer bizType) {
        Page<ItemStorageIn> page = new Page<>(pageNo, pageSize);
        return Result.OK(itemStorageInService.queryList(page, AppAuthUtil.getUserInfo().getId(), 1, bizType));
    }

    @Operation(summary = "待我接收/我接收的移库单列表")
    @GetMapping("/move/list")
    public Result<Page<StorageInListVO>> moveList(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<ItemStorageIn> page = new Page<>(pageNo, pageSize);
        return Result.OK(itemStorageInService.queryList(page, AppAuthUtil.getUserInfo().getId(), 2, null));
    }

    @Operation(summary = "单据详情（转入/移库通用）")
    @GetMapping("/transferIn/detail")
    public Result<StorageInDetailVO> detail(@RequestParam String recordId) {
        return Result.OK(itemStorageInService.queryDetail(AppAuthUtil.getUserInfo().getId(), recordId));
    }

    // ==================== 发运（赠送领取后快递寄送） ====================

    @Operation(summary = "接收方确认落仓信息并触发快递下单")
    @PostMapping("/move/receiverConfirm")
    public Result<ItemStorageIn> moveReceiverConfirm(@RequestBody MoveReceiverConfirmReqVO reqVO) {
        return Result.OK(itemStorageInService.moveReceiverConfirm(AppAuthUtil.getUserInfo().getId(), reqVO));
    }

    @Operation(summary = "寄件方标记线下当面交付（30→40，待接收方确认收货）")
    @PostMapping("/move/offlineDeliver")
    public Result<ItemStorageIn> moveOfflineDeliver(@RequestParam String recordId) {
        return Result.OK(itemStorageInService.moveOfflineDeliver(AppAuthUtil.getUserInfo().getId(), recordId));
    }

    @Operation(summary = "接收方确认线下收货并选择入库仓（40→33）")
    @PostMapping("/move/offlineConfirm")
    public Result<ItemStorageIn> moveOfflineConfirm(@RequestBody MoveReceiverConfirmReqVO reqVO) {
        return Result.OK(itemStorageInService.moveOfflineConfirm(AppAuthUtil.getUserInfo().getId(), reqVO));
    }

    @Operation(summary = "接收方手动确认签收（31/32→33）")
    @PostMapping("/move/receiverSign")
    public Result<ItemStorageIn> receiverSign(@RequestParam String recordId) {
        return Result.OK(itemStorageInService.receiverSign(AppAuthUtil.getUserInfo().getId(), recordId));
    }
}
