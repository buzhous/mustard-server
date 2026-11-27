package org.jeecg.modules.admin.ailing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.admin.ailing.service.AilingItemAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * 芥灵AI 物品只读工作台（P2-4）：/admin/ailing/item/**
 * 列表（normal/recycle）/ 详情（restricted 脱敏）/ 时间线（账本 item_id 直查 + spawn + chainValid）/
 * 统计 / 检索索引异步全量重建（按钮权限 ailing:rebuild 由菜单控制）
 */
@Slf4j
@RestController
@RequestMapping("/admin/ailing/item")
@Tag(name = "芥灵AI物品工作台")
public class AilingItemAdminController {

    @Autowired
    private AilingItemAdminService ailingItemAdminService;

    @Operation(summary = "物品分页列表（view=normal 在库 / view=recycle 回收站）")
    @GetMapping(value = "/list")
    public Result<?> list(@RequestParam(name = "view", defaultValue = "normal") String view,
                          @RequestParam(name = "itemId", required = false) String itemId,
                          @RequestParam(name = "keyword", required = false) String keyword,
                          @RequestParam(name = "category", required = false) String category,
                          @RequestParam(name = "tag", required = false) String tag,
                          @RequestParam(name = "level", required = false) Integer level,
                          @RequestParam(name = "status", required = false) Integer status,
                          @RequestParam(name = "stationId", required = false) String stationId,
                          @RequestParam(name = "priceMin", required = false) BigDecimal priceMin,
                          @RequestParam(name = "priceMax", required = false) BigDecimal priceMax,
                          @RequestParam(name = "createTimeStart", required = false)
                          @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTimeStart,
                          @RequestParam(name = "createTimeEnd", required = false)
                          @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTimeEnd,
                          @RequestParam(name = "source", required = false) String source,
                          @RequestParam(name = "uid", required = false) String uid,
                          @RequestParam(name = "oriId", required = false) String oriId,
                          @RequestParam(name = "orderBy", required = false) String orderBy,
                          @RequestParam(name = "orderDir", required = false) String orderDir,
                          @RequestParam(name = "pageNo", defaultValue = "1") long pageNo,
                          @RequestParam(name = "pageSize", defaultValue = "10") long pageSize) {
        return Result.ok(ailingItemAdminService.list(view, itemId, keyword, category, tag, level, status,
                stationId, priceMin, priceMax, createTimeStart, createTimeEnd, source, uid, oriId,
                orderBy, orderDir, pageNo, pageSize));
    }

    @Operation(summary = "物品详情（全字段 + restricted 脱敏「手动补录」）")
    @GetMapping(value = "/detail")
    public Result<?> detail(@RequestParam(name = "itemId") String itemId) {
        Map<String, Object> detail = ailingItemAdminService.detail(itemId);
        if (detail == null) {
            return Result.error("未找到对应物品");
        }
        return Result.ok(detail);
    }

    @Operation(summary = "流转时间线（账本 item_id 直查 + spawn 父子链 + chainValid）")
    @GetMapping(value = "/timeline")
    public Result<?> timeline(@RequestParam(name = "itemId") String itemId) {
        return Result.ok(ailingItemAdminService.timeline(itemId));
    }

    @Operation(summary = "物品统计（总量/用户数/今日新增/AI占比/分类TOP/标签TOP/等级/趋势/来源）")
    @GetMapping(value = "/stats")
    public Result<?> stats() {
        return Result.ok(ailingItemAdminService.stats());
    }

    @Operation(summary = "检索索引异步全量重建（单线程，幂等；已在跑返回 running 状态）")
    @PostMapping(value = "/searchIndex/rebuild")
    public Result<?> rebuildSearchIndex() {
        boolean started = ailingItemAdminService.startRebuild();
        if (!started) {
            return Result.ok(ailingItemAdminService.rebuildProgress());
        }
        return Result.ok("重建任务已启动");
    }

    @Operation(summary = "检索索引重建进度（total/done/fail/running）")
    @GetMapping(value = "/searchIndex/progress")
    public Result<?> rebuildProgress() {
        return Result.ok(ailingItemAdminService.rebuildProgress());
    }
}
