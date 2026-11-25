package org.jeecg.modules.ai.controller;

import cn.hutool.core.util.ObjectUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.ai.bean.enums.AiErrorEnum;
import org.jeecg.modules.ai.entity.AiChatMessage;
import org.jeecg.modules.ai.entity.AiChatSession;
import org.jeecg.modules.ai.port.AiAuthPort;
import org.jeecg.modules.ai.service.IAiChatMessageService;
import org.jeecg.modules.ai.service.IAiChatSessionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 建档会话域接口（ai-integration-plan §5.9 S2）
 * 创建会话 / 会话列表 / 恢复消息流；发送与创建调度见 AiAgentController（S3–S5）。
 */
@Slf4j
@RestController
@RequestMapping("/app/ai/chat")
@Tag(name = "App-AI建档会话")
public class AppAiChatController {

    private final IAiChatSessionService sessionService;
    private final IAiChatMessageService messageService;
    private final AiAuthPort authPort;

    public AppAiChatController(IAiChatSessionService sessionService, IAiChatMessageService messageService,
                               AiAuthPort authPort) {
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.authPort = authPort;
    }

    @Operation(summary = "创建新会话")
    @PostMapping("/session/create")
    public Result<AiChatSession> createSession() {
        String uid = authPort.currentUid();
        if (ObjectUtil.isEmpty(uid)) {
            return Result.error(AiErrorEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        return Result.OK(sessionService.createSession(uid));
    }

    @Operation(summary = "会话列表（状态点+标题+摘要+时间）")
    @GetMapping("/session/list")
    public Result<List<AiChatSession>> listSessions() {
        String uid = authPort.currentUid();
        if (ObjectUtil.isEmpty(uid)) {
            return Result.error(AiErrorEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        return Result.OK(sessionService.listByUid(uid));
    }

    @Operation(summary = "恢复消息流（切换/重开完整恢复）")
    @GetMapping("/message/list")
    public Result<List<AiChatMessage>> listMessages(@RequestParam(name = "sessionId") String sessionId) {
        String uid = authPort.currentUid();
        if (ObjectUtil.isEmpty(uid)) {
            return Result.error(AiErrorEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        AiChatSession session = sessionService.getOwned(uid, sessionId);
        if (session == null) {
            return Result.error(AiErrorEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        return Result.OK(messageService.listBySession(uid, sessionId));
    }

    @Operation(summary = "重命名会话")
    @PostMapping("/session/rename")
    public Result<AiChatSession> renameSession(@RequestParam(name = "sessionId") String sessionId,
                                               @RequestParam(name = "title") String title) {
        String uid = authPort.currentUid();
        if (ObjectUtil.isEmpty(uid)) {
            return Result.error(AiErrorEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        return Result.OK(sessionService.rename(uid, sessionId, title));
    }

    @Operation(summary = "置顶/取消置顶会话")
    @PostMapping("/session/pin")
    public Result<AiChatSession> pinSession(@RequestParam(name = "sessionId") String sessionId,
                                            @RequestParam(name = "pinned") boolean pinned) {
        String uid = authPort.currentUid();
        if (ObjectUtil.isEmpty(uid)) {
            return Result.error(AiErrorEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        return Result.OK(sessionService.setPinned(uid, sessionId, pinned));
    }

    @Operation(summary = "删除会话（软删，消息随会话不可见）")
    @PostMapping("/session/delete")
    public Result<String> deleteSession(@RequestParam(name = "sessionId") String sessionId) {
        String uid = authPort.currentUid();
        if (ObjectUtil.isEmpty(uid)) {
            return Result.error(AiErrorEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        sessionService.softDelete(uid, sessionId);
        return Result.OK("删除成功");
    }
}
