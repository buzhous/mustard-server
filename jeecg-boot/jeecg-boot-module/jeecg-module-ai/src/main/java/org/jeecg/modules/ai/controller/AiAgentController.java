package org.jeecg.modules.ai.controller;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.ai.adapter.AiRateLimiter;
import org.jeecg.modules.ai.bean.enums.AiErrorEnum;
import org.jeecg.modules.ai.bean.vo.ai.AiChatCreateVO;
import org.jeecg.modules.ai.bean.vo.ai.AiChatSendVO;
import org.jeecg.modules.ai.port.AiAuthPort;
import org.jeecg.modules.ai.service.AiAgentService;
import org.jeecg.modules.ai.stream.AiStreamService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * AI 建档编排端点（ai-integration-plan §5.9 S3/S4/S5）
 * send：发消息 → 解析 → 草稿 JSON + 置信度 / 追问；create：确认创建 → 数量 N → MCP item_create ×N。
 * send/stream：L1 SSE 阶段流主通道（session/progress/message/done/error，契约见 docs/ai-sse-infrastructure-evaluation.md §三；
 * delta/surface 事件为契约预留位，本期不实现）。鉴权/参数校验/限流前置在 servlet 线程完成后交 AiStreamService。
 */
@Slf4j
@RestController
@RequestMapping("/app/ai/chat")
@Tag(name = "App-AI建档编排")
public class AiAgentController {

    private final AiAgentService agentService;
    private final AiAuthPort authPort;
    private final AiRateLimiter rateLimiter;
    private final AiStreamService aiStreamService;
    private final org.jeecg.modules.ai.service.AiUpdateService updateService;

    public AiAgentController(AiAgentService agentService, AiAuthPort authPort,
                             AiRateLimiter rateLimiter, AiStreamService aiStreamService,
                             org.jeecg.modules.ai.service.AiUpdateService updateService) {
        this.agentService = agentService;
        this.authPort = authPort;
        this.rateLimiter = rateLimiter;
        this.aiStreamService = aiStreamService;
        this.updateService = updateService;
    }

    @Operation(summary = "发送消息（解析→草稿/追问/闲聊）")
    @PostMapping("/send")
    public Result<AiChatSendVO> send(@RequestBody JSONObject body) {
        String uid = authPort.currentUid();
        if (ObjectUtil.isEmpty(uid)) {
            return Result.error(AiErrorEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        if (body == null || ObjectUtil.isEmpty(body.getString("sessionId"))) {
            return Result.error(AiErrorEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        List<String> images = body.getJSONArray("images") == null
                ? null : body.getJSONArray("images").toJavaList(String.class);
        // mode 可选：normal（默认对话建档）| express（快速建档，免确认直达）| mock（模拟建档，虚构补齐草稿）
        return Result.OK(agentService.send(uid, body.getString("sessionId"),
                body.getString("text"), images, body.getString("mode")));
    }

    @Operation(summary = "发送消息（SSE 阶段流主通道：session/progress/message/done/error）")
    @PostMapping(value = "/send/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendStream(@RequestBody JSONObject body, HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        // servlet 线程内完成鉴权/参数校验/开关/限流前置（异步线程无请求上下文）；
        // 任一失败直接回 Result 错误 JSON（App 检测非 event-stream 自动落回一次性 POST），不建 emitter
        String failMessage = null;
        String uid = authPort.currentUid();
        if (ObjectUtil.isEmpty(uid)) {
            failMessage = AiErrorEnum.USER_INFO_NOT_EXIST.getMsg();
        } else if (body == null || ObjectUtil.isEmpty(body.getString("sessionId"))) {
            failMessage = AiErrorEnum.REQUEST_PARAM_ERROR.getMsg();
        } else if (!aiStreamService.isEnabled()) {
            failMessage = "流式通道未开启";
        } else {
            String limitError = rateLimiter.tryAcquireChat(uid);
            if (limitError != null) {
                failMessage = limitError;
            }
        }
        if (failMessage != null) {
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write(JSON.toJSONString(Result.error(failMessage)));
            } catch (IOException e) {
                log.warn("[ai-agent] stream 错误响应写出失败: {}", e.getMessage());
            }
            return null;
        }
        List<String> images = body.getJSONArray("images") == null
                ? null : body.getJSONArray("images").toJavaList(String.class);
        return aiStreamService.open(uid, body.getString("sessionId"),
                body.getString("text"), images, body.getString("mode"));
    }

    @Operation(summary = "确认创建（数量 1-99，默认 1）")
    @PostMapping("/create")
    public Result<AiChatCreateVO> create(@RequestBody JSONObject body) {
        String uid = authPort.currentUid();
        if (ObjectUtil.isEmpty(uid)) {
            return Result.error(AiErrorEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        if (body == null || ObjectUtil.isEmpty(body.getString("sessionId"))) {
            return Result.error(AiErrorEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        // draft 兼容两种形态：App 传嵌套对象 / 外部传 JSON 字符串；
        // fastjson2 兼容层 getString 对嵌套对象返回 LinkedHashMap toString，需显式 toJSONString
        Object draftParam = body.get("draft");
        String draftJson = draftParam == null ? null
                : draftParam instanceof String ? (String) draftParam : JSON.toJSONString(draftParam);
        return Result.OK(agentService.confirmCreate(uid,
                body.getString("sessionId"), body.getInteger("quantity"), draftJson));
    }

    @Operation(summary = "确认更新（P5 update 意图：按更新对照卡执行编辑）")
    @PostMapping("/update")
    public Result<AiChatSendVO> update(@RequestBody JSONObject body) {
        String uid = authPort.currentUid();
        if (ObjectUtil.isEmpty(uid)) {
            return Result.error(AiErrorEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        if (body == null || ObjectUtil.isEmpty(body.getString("sessionId"))
                || ObjectUtil.isEmpty(body.getString("messageId"))) {
            return Result.error(AiErrorEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        return Result.OK(updateService.confirmUpdate(uid,
                body.getString("sessionId"), body.getString("messageId")));
    }
}
