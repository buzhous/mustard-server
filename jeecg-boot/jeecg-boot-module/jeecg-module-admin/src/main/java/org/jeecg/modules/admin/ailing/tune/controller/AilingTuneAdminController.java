package org.jeecg.modules.admin.ailing.tune.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.modules.admin.ailing.tune.service.AilingAliasAdminService;
import org.jeecg.modules.admin.ailing.tune.service.AilingTuneAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 芥灵AI 调优中心（P4）：/admin/ailing/tune/**
 * 提示词在线编辑（DB enabled &gt; 内置技能包）/ 模型与检索参数下发 / 兜底链观测 / 词表维护 / 样本池处置。
 * 读接口随菜单权限；写接口按钮权限 ailing:tune:edit（限管理员角色）。
 */
@Slf4j
@RestController
@RequestMapping("/admin/ailing/tune")
@Tag(name = "芥灵AI调优中心")
public class AilingTuneAdminController {

    @Autowired
    private AilingTuneAdminService tuneAdminService;
    @Autowired
    private AilingAliasAdminService aliasAdminService;

    @Operation(summary = "全量配置视图（技能包/模型参数/检索参数/版本历史）")
    @GetMapping(value = "/config")
    public Result<?> config() {
        return Result.ok(tuneAdminService.configView());
    }

    @Operation(summary = "提示词保存（占位符完整性校验，新版本写入即刻生效）")
    @RequiresPermissions("ailing:tune:edit")
    @PostMapping(value = "/prompt/save")
    public Result<?> promptSave(@RequestBody JSONObject body,
                                @RequestHeader(value = "X-Access-Token", required = false) String token) {
        try {
            return Result.ok(tuneAdminService.promptSave(body.getString("skill"), body.getString("content"),
                    operator(token)));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "提示词一键回滚内置（该 key 全部 DB 行停用）")
    @RequiresPermissions("ailing:tune:edit")
    @PostMapping(value = "/prompt/rollback")
    public Result<?> promptRollback(@RequestBody JSONObject body,
                                    @RequestHeader(value = "X-Access-Token", required = false) String token) {
        try {
            return Result.ok(tuneAdminService.promptRollback(body.getString("skill"), operator(token)));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "恢复提示词历史版本（追加新 active 版本，审计可追溯）")
    @RequiresPermissions("ailing:tune:edit")
    @PostMapping(value = "/prompt/restore")
    public Result<?> promptRestore(@RequestBody JSONObject body,
                                   @RequestHeader(value = "X-Access-Token", required = false) String token) {
        try {
            return Result.ok(tuneAdminService.promptRestore(body.getString("skill"), body.getInteger("version"),
                    operator(token)));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "模型与检索参数下发（chat_model/temperature/max_asks/alias/index/cache 开关与 TTL）")
    @RequiresPermissions("ailing:tune:edit")
    @PostMapping(value = "/params/save")
    public Result<?> paramsSave(@RequestBody JSONObject body,
                                @RequestHeader(value = "X-Access-Token", required = false) String token) {
        try {
            return Result.ok(tuneAdminService.paramsSave(body.getJSONObject("model"), body.getJSONObject("search"),
                    operator(token)));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "兜底链观测统计（近 N 天：环节命中率/耗时/缓存命中/LLM 兜底占比）")
    @GetMapping(value = "/observe/stats")
    public Result<?> observeStats(@RequestParam(name = "days", defaultValue = "7") int days) {
        return Result.ok(tuneAdminService.observeStats(days));
    }

    @Operation(summary = "低命中样本列表（最近 200 条）")
    @GetMapping(value = "/observe/misses")
    public Result<?> observeMisses() {
        return Result.ok(tuneAdminService.observeMisses());
    }

    @Operation(summary = "词表分页（alias/standard 模糊 + 类型/启用筛选）")
    @GetMapping(value = "/alias/list")
    public Result<?> aliasList(@RequestParam(name = "pageNo", defaultValue = "1") long pageNo,
                               @RequestParam(name = "pageSize", defaultValue = "10") long pageSize,
                               @RequestParam(name = "alias", required = false) String alias,
                               @RequestParam(name = "standard", required = false) String standard,
                               @RequestParam(name = "aliasType", required = false) Integer aliasType,
                               @RequestParam(name = "enabled", required = false) Integer enabled) {
        return Result.ok(aliasAdminService.list(pageNo, pageSize, alias, standard, aliasType, enabled));
    }

    @Operation(summary = "词表新增/更新（uk_alias 冲突友好报错；写后删词表缓存）")
    @RequiresPermissions("ailing:tune:edit")
    @PostMapping(value = "/alias/save")
    public Result<?> aliasSave(@RequestBody JSONObject body,
                               @RequestHeader(value = "X-Access-Token", required = false) String token) {
        try {
            return Result.ok(aliasAdminService.save(body.getString("id"), body.getString("alias"),
                    body.getString("standard"), body.getInteger("aliasType"), body.getInteger("enabled"),
                    operator(token)));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "词表删除（物理删除，写后删词表缓存）")
    @RequiresPermissions("ailing:tune:edit")
    @PostMapping(value = "/alias/delete")
    public Result<?> aliasDelete(@RequestBody JSONObject body) {
        try {
            return Result.ok(aliasAdminService.delete(body.getString("id")));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "样本池分页（来源/状态/用户/时间筛选）")
    @GetMapping(value = "/feedback/list")
    public Result<?> feedbackList(@RequestParam(name = "pageNo", defaultValue = "1") long pageNo,
                                  @RequestParam(name = "pageSize", defaultValue = "10") long pageSize,
                                  @RequestParam(name = "sourceType", required = false) Integer sourceType,
                                  @RequestParam(name = "sampleStatus", required = false) Integer sampleStatus,
                                  @RequestParam(name = "uid", required = false) String uid,
                                  @RequestParam(name = "timeStart", required = false) String timeStart,
                                  @RequestParam(name = "timeEnd", required = false) String timeEnd) {
        return Result.ok(tuneAdminService.feedbackList(pageNo, pageSize, sourceType, sampleStatus, uid, timeStart, timeEnd));
    }

    @Operation(summary = "样本处置（alias=补词表 / ignore=忽略 / baseline=导出基准用例 JSON）")
    @RequiresPermissions("ailing:tune:edit")
    @PostMapping(value = "/feedback/handle")
    public Result<?> feedbackHandle(@RequestBody JSONObject body,
                                    @RequestHeader(value = "X-Access-Token", required = false) String token) {
        try {
            return Result.ok(tuneAdminService.feedbackHandle(body.getString("id"), body.getString("action"),
                    body.getString("standard"), body.getInteger("aliasType"), operator(token)));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 操作人：X-Access-Token 解析（JwtUtil），失败记 unknown（审计字段，不阻断主流程） */
    private String operator(String token) {
        if (token == null || token.isBlank()) {
            return "unknown";
        }
        try {
            String username = JwtUtil.getUsername(token);
            return username == null || username.isBlank() ? "unknown" : username;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
