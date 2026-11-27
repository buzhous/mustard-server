package org.jeecg.modules.admin.ailing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.modules.admin.ailing.service.AilingSessionAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;

/**
 * 芥灵过程 · AI 会话可视化（P3-2）：/admin/ailing/session/**
 * 会话列表 / 会话回放（仅客服/运营角色，ailing:session:view，读取即审计）/ 质量看板 / 审计查询。
 * 全部只读。
 */
@Slf4j
@RestController
@RequestMapping("/admin/ailing/session")
@Tag(name = "芥灵AI过程可视化")
public class AilingSessionAdminController {

    @Autowired
    private AilingSessionAdminService ailingSessionAdminService;

    @Operation(summary = "会话分页列表（附消息数/模式/最后草稿置信度）")
    @GetMapping(value = "/list")
    public Result<?> list(@RequestParam(name = "sessionId", required = false) String sessionId,
                          @RequestParam(name = "uid", required = false) String uid,
                          @RequestParam(name = "username", required = false) String username,
                          @RequestParam(name = "title", required = false) String title,
                          @RequestParam(name = "status", required = false) String status,
                          @RequestParam(name = "mode", required = false) String mode,
                          @RequestParam(name = "createTimeStart", required = false)
                          @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTimeStart,
                          @RequestParam(name = "createTimeEnd", required = false)
                          @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTimeEnd,
                          @RequestParam(name = "pageNo", defaultValue = "1") long pageNo,
                          @RequestParam(name = "pageSize", defaultValue = "10") long pageSize) {
        return Result.ok(ailingSessionAdminService.list(sessionId, uid, username, title, status, mode,
                createTimeStart, createTimeEnd, pageNo, pageSize));
    }

    @Operation(summary = "会话详情回放（session + messages；读取即审计落 tbl_stats_event）")
    @RequiresPermissions("ailing:session:view")
    @GetMapping(value = "/detail")
    public Result<?> detail(@RequestParam(name = "sessionId") String sessionId, HttpServletRequest request) {
        String viewer = currentUserName(request);
        Map<String, Object> detail = ailingSessionAdminService.detail(sessionId, viewer);
        if (detail == null) {
            return Result.error("未找到对应会话");
        }
        return Result.ok(detail);
    }

    @Operation(summary = "质量看板（建档成功率/express直达率/平均置信度/追问轮次分布/趋势/模式分布）")
    @GetMapping(value = "/stats")
    public Result<?> stats() {
        return Result.ok(ailingSessionAdminService.stats());
    }

    @Operation(summary = "会话回放查看审计（tbl_stats_event ai_session_viewed 最近 200 条）")
    @RequiresPermissions("ailing:session:view")
    @GetMapping(value = "/audit/list")
    public Result<?> auditList() {
        return Result.ok(ailingSessionAdminService.auditList());
    }

    /** 当前登录管理员账号（X-Access-Token 解析，失败返回空串，不阻塞主流程） */
    private String currentUserName(HttpServletRequest request) {
        try {
            String name = JwtUtil.getUserNameByToken(request);
            return name == null ? "" : name;
        } catch (Exception e) {
            return "";
        }
    }
}
