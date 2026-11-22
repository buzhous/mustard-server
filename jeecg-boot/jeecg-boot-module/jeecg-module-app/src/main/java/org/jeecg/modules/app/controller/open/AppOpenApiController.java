package org.jeecg.modules.app.controller.open;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.vo.item.ItemInfoQueryVO;
import org.jeecg.modules.app.entity.ItemInfo;
import org.jeecg.modules.app.entity.OpenAppCredential;
import org.jeecg.modules.app.openapi.OpenSignatureFilter;
import org.jeecg.modules.app.service.AppOpenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 芥子开放平台数据接口（/app/open/**）
 *
 * 鉴权：OpenSignatureFilter 统一守门（appkey 请求头 → 启用凭据 → IP 白名单 → scope 分级 → 限流 → 审计），
 * 凭据经 request attribute {@link OpenSignatureFilter#ATTR_CREDENTIAL} 透传到本控制器。
 * 数据范围：凭据绑定的用户（open_app_credentials.bound_uid）的物品数据。
 * MIP 出域红线在 AppOpenService 内核完成（出口层不做手写字段过滤）。
 */
@Slf4j
@RestController
@RequestMapping("/app/open")
@Tag(name = "Open-开放平台数据接口")
public class AppOpenApiController {

    @Autowired
    private AppOpenService appOpenService;

    @Autowired
    private org.jeecg.modules.app.service.ItemFlowService itemFlowService;

    @Operation(summary = "物品分页查询（含 MIP 出域摘要 schemaId/contentHash）")
    @GetMapping("/item/list")
    public Result<Page<ItemInfo>> itemList(
            HttpServletRequest request,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "stationId", required = false) String stationId,
            @RequestParam(name = "stationType", required = false) Integer stationType,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "orderBy", required = false) String orderBy,
            @RequestParam(name = "ascending", required = false, defaultValue = "false") Boolean ascending) {
        OpenAppCredential credential = credential(request);
        ItemInfoQueryVO queryVO = new ItemInfoQueryVO()
                .setPageNo(pageNo)
                .setPageSize(pageSize)
                .setKeyword(keyword)
                .setStationId(stationId)
                .setStationType(stationType)
                .setStatus(status)
                .setOrderBy(orderBy)
                .setAscending(ascending);
        return Result.OK(appOpenService.pageItems(credential.getBoundUid(), queryVO));
    }

    @Operation(summary = "物品详情（MIP 实例视图，红线过滤）")
    @GetMapping("/item/detail")
    public Result<JSONObject> itemDetail(
            HttpServletRequest request,
            @RequestParam(name = "itemId") String itemId) {
        OpenAppCredential credential = credential(request);
        if (StrUtil.isEmpty(itemId)) {
            return Result.error(400, "itemId 不能为空");
        }
        JSONObject instance = appOpenService.itemDetail(credential.getBoundUid(), itemId);
        if (instance == null) {
            return Result.error(404, "item not found");
        }
        return Result.OK(instance);
    }

    @Operation(summary = "物品搜索（轻量摘要，keyword 必填）")
    @GetMapping("/item/search")
    public Result<JSONObject> itemSearch(
            HttpServletRequest request,
            @RequestParam(name = "keyword") String keyword,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "fields", required = false) String fields) {
        OpenAppCredential credential = credential(request);
        if (StrUtil.isEmpty(keyword)) {
            return Result.error(400, "keyword 不能为空");
        }
        JSONObject out = new JSONObject(true);
        out.put("keyword", keyword);
        out.put("items", appOpenService.searchItems(credential.getBoundUid(), keyword, limit, fields));
        return Result.OK(out);
    }

    @Operation(summary = "物品统计（仅聚合值）")
    @GetMapping("/item/stats")
    public Result<JSONObject> itemStats(HttpServletRequest request) {
        OpenAppCredential credential = credential(request);
        return Result.OK(appOpenService.stats(credential.getBoundUid()));
    }

    @Operation(summary = "流转记录（账本哈希链校验）")
    @GetMapping("/item/lineage")
    public Result<JSONObject> itemLineage(
            HttpServletRequest request,
            @RequestParam(name = "itemId") String itemId) {
        OpenAppCredential credential = credential(request);
        if (StrUtil.isEmpty(itemId)) {
            return Result.error(400, "itemId 不能为空");
        }
        JSONObject lineage = appOpenService.lineage(credential.getBoundUid(), itemId);
        if (lineage == null) {
            return Result.error(404, "item not found");
        }
        return Result.OK(lineage);
    }

    @Operation(summary = "Agent 新增物品（readwrite；X-Idempotency-Key 必填）")
    @PostMapping("/item/add")
    public Result<JSONObject> itemAdd(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody JSONObject body) {
        OpenAppCredential credential = credential(request);
        if (StrUtil.isEmpty(idempotencyKey)) {
            return withStatus(response, Result.error(400, "missing idempotency key"));
        }
        return withStatus(response, appOpenService.addItem(credential, body, idempotencyKey));
    }

    @Operation(summary = "编辑物品（readwrite；MIP 写入链，账本 update 事件，幂等回放）")
    @PostMapping("/item/update")
    public Result<JSONObject> itemUpdate(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody JSONObject body) {
        OpenAppCredential credential = credential(request);
        if (StrUtil.isEmpty(idempotencyKey)) {
            return withStatus(response, Result.error(400, "missing idempotency key"));
        }
        return withStatus(response, appOpenService.updateItem(credential, body, idempotencyKey));
    }

    @Operation(summary = "删除物品（readwrite；高危不可恢复，账本 recycle 事件，幂等回放）")
    @PostMapping("/item/delete")
    public Result<JSONObject> itemDelete(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody JSONObject body) {
        OpenAppCredential credential = credential(request);
        if (StrUtil.isEmpty(idempotencyKey)) {
            return withStatus(response, Result.error(400, "missing idempotency key"));
        }
        return withStatus(response, appOpenService.deleteItem(credential, body, idempotencyKey));
    }

    @Operation(summary = "赠送预览（readwrite；返回一次性 giftToken，TTL 300s）")
    @PostMapping("/gift/preview")
    public Result<JSONObject> giftPreview(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody JSONObject body) {
        OpenAppCredential credential = credential(request);
        String itemId = body == null ? null : body.getString("itemId");
        if (StrUtil.isEmpty(itemId)) {
            return withStatus(response, Result.error(400, "itemId 不能为空"));
        }
        return withStatus(response, appOpenService.giftPreview(credential, itemId));
    }

    @Operation(summary = "赠送执行（readwrite；giftToken 一次性，赠送不可撤销）")
    @PostMapping("/gift/send")
    public Result<JSONObject> giftSend(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody JSONObject body) {
        OpenAppCredential credential = credential(request);
        if (body == null) {
            return withStatus(response, Result.error(400, "请求体不能为空"));
        }
        return withStatus(response, appOpenService.giftSend(credential, body.getString("giftToken"), body.getString("toUserId")));
    }

    // ================================ D1 定向流转（两阶段 offer/accept） ================================

    @Operation(summary = "流转要约发起（readwrite；生成 flowToken 绑定接收方，TTL 72h，锁定期拒 gift/update/delete）")
    @PostMapping("/flow/offer")
    public Result<JSONObject> flowOffer(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody JSONObject body) {
        OpenAppCredential credential = credential(request);
        if (StrUtil.isEmpty(idempotencyKey)) {
            return withStatus(response, Result.error(400, "missing idempotency key"));
        }
        return withStatus(response, itemFlowService.offer(credential, body, idempotencyKey));
    }

    @Operation(summary = "流转确认（readwrite；仅接收方本人凭据可确认，账本 flow_out/flow_in 事件对）")
    @PostMapping("/flow/accept")
    public Result<JSONObject> flowAccept(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody JSONObject body) {
        OpenAppCredential credential = credential(request);
        if (StrUtil.isEmpty(idempotencyKey)) {
            return withStatus(response, Result.error(400, "missing idempotency key"));
        }
        return withStatus(response, itemFlowService.accept(credential,
                body == null ? null : body.getString("flowToken"), idempotencyKey));
    }

    @Operation(summary = "流转拒绝（readwrite；仅接收方本人凭据，解锁回退不记账）")
    @PostMapping("/flow/reject")
    public Result<JSONObject> flowReject(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody JSONObject body) {
        OpenAppCredential credential = credential(request);
        if (StrUtil.isEmpty(idempotencyKey)) {
            return withStatus(response, Result.error(400, "missing idempotency key"));
        }
        return withStatus(response, itemFlowService.reject(credential,
                body == null ? null : body.getString("flowToken"), idempotencyKey));
    }

    @Operation(summary = "流转撤回（readwrite；仅发起方凭据，解锁回退不记账）")
    @PostMapping("/flow/cancel")
    public Result<JSONObject> flowCancel(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody JSONObject body) {
        OpenAppCredential credential = credential(request);
        if (StrUtil.isEmpty(idempotencyKey)) {
            return withStatus(response, Result.error(400, "missing idempotency key"));
        }
        return withStatus(response, itemFlowService.cancel(credential,
                body == null ? null : body.getString("flowToken"), idempotencyKey));
    }

    @Operation(summary = "流转要约列表（发出/收到两个方向，仅待处理；已完成以账本事件为准）")
    @GetMapping("/flow/list")
    public Result<JSONObject> flowList(
            HttpServletRequest request,
            @RequestParam(name = "dir", required = false) String dir) {
        OpenAppCredential credential = credential(request);
        return Result.OK(itemFlowService.list(credential.getBoundUid(), dir));
    }

    /**
     * HTTP 状态与业务 code 同步（设计方案 3.6：422 携带校验明细、403/404 状态同步）
     */
    private Result<JSONObject> withStatus(HttpServletResponse response, Result<JSONObject> result) {
        Integer code = result.getCode();
        if (code != null && code >= 400) {
            response.setStatus(code);
        }
        return result;
    }

    /**
     * 取 Filter 层鉴权通过的凭据（Filter 已完成 appkey/白名单/scope/限流）
     */
    private OpenAppCredential credential(HttpServletRequest request) {
        return (OpenAppCredential) request.getAttribute(OpenSignatureFilter.ATTR_CREDENTIAL);
    }
}
