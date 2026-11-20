package org.jeecg.modules.app.controller.kuaidi;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.storagein.StorageInDetailVO;
import org.jeecg.modules.app.entity.KuaidiOrder;
import org.jeecg.modules.app.service.IItemStorageInService;
import org.jeecg.modules.app.service.Kuaidi100Service;
import org.jeecg.modules.app.utils.AppAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 快递100 回调与查询
 * /app/kuaidi/callback 在 AppAuthFilter 白名单（快递100 服务器推送无 App token）
 *
 * @author elinx
 */
@Slf4j
@Tag(name = "快递100")
@RestController
@RequestMapping("/app/kuaidi")
public class AppKuaidiController {

    @Autowired
    private Kuaidi100Service kuaidi100Service;

    @Autowired
    private IItemStorageInService itemStorageInService;

    /**
     * 快递100 订单状态推送回调
     * 兼容 JSON body 与 form(param) 两种推送格式，提取 taskId/status/kuaidinum
     */
    @Operation(summary = "快递100状态回调(免鉴权)")
    @PostMapping("/callback")
    public Result<String> callback(@RequestBody(required = false) String body,
                                   @RequestParam(required = false) String param,
                                   @RequestParam(required = false) String taskId,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String kuaidinum) {
        log.info("快递100回调 body={}, param={}, taskId={}, status={}", body, param, taskId, status);
        JSONObject json = null;
        try {
            if (StrUtil.isNotBlank(body)) {
                json = JSONUtil.parseObj(body);
            } else if (StrUtil.isNotBlank(param)) {
                json = JSONUtil.parseObj(param);
            }
        } catch (Exception ignored) {
            // 非 JSON 推送，忽略
        }
        String tId = json == null ? taskId : json.getStr("taskId", json.getStr("taskid", taskId));
        String st = json == null ? status : json.getStr("status", status);
        String num = json == null ? kuaidinum : json.getStr("kuaidinum", kuaidinum);
        if (StrUtil.isBlank(tId) || StrUtil.isBlank(st)) {
            return Result.error("缺少 taskId/status");
        }
        KuaidiOrder order = kuaidi100Service.getByTaskId(tId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        int newStatus;
        try {
            newStatus = Integer.parseInt(st);
        } catch (NumberFormatException e) {
            return Result.error("status 格式错误");
        }
        kuaidi100Service.updateOrderStatus(order, newStatus, num, body != null ? body : param);
        itemStorageInService.onKuaidiStatus(order, newStatus);
        return Result.OK("ok");
    }

    @Operation(summary = "轮询快递订单状态")
    @GetMapping("/poll")
    public Result<KuaidiOrder> poll(@RequestParam String recordId) {
        // 鉴权：仅发起人/接收人可查
        StorageInDetailVO detail = itemStorageInService.queryDetail(AppAuthUtil.getUserInfo().getId(), recordId);
        if (detail == null) {
            throw new AppException(ExceptionEnum.STORAGE_IN_NOT_EXIST);
        }
        return Result.OK(kuaidi100Service.getLatestByRecordId(recordId));
    }
}
