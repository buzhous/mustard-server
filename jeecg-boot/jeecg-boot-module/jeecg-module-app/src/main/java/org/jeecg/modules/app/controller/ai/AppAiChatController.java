package org.jeecg.modules.app.controller.ai;

import cn.hutool.core.util.ObjectUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.entity.AiChatMessage;
import org.jeecg.modules.app.entity.AiChatSession;
import org.jeecg.modules.app.entity.user.AppUser;
import org.jeecg.modules.app.service.IAiChatMessageService;
import org.jeecg.modules.app.service.IAiChatSessionService;
import org.jeecg.modules.app.utils.AppAuthUtil;
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

    public AppAiChatController(IAiChatSessionService sessionService, IAiChatMessageService messageService) {
        this.sessionService = sessionService;
        this.messageService = messageService;
    }

    @Operation(summary = "创建新会话")
    @PostMapping("/session/create")
    public Result<AiChatSession> createSession() {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error(ExceptionEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        return Result.OK(sessionService.createSession(loginUser.getId()));
    }

    @Operation(summary = "会话列表（状态点+标题+摘要+时间）")
    @GetMapping("/session/list")
    public Result<List<AiChatSession>> listSessions() {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error(ExceptionEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        return Result.OK(sessionService.listByUid(loginUser.getId()));
    }

    @Operation(summary = "恢复消息流（切换/重开完整恢复）")
    @GetMapping("/message/list")
    public Result<List<AiChatMessage>> listMessages(@RequestParam(name = "sessionId") String sessionId) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error(ExceptionEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        AiChatSession session = sessionService.getOwned(loginUser.getId(), sessionId);
        if (session == null) {
            return Result.error(ExceptionEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        return Result.OK(messageService.listBySession(loginUser.getId(), sessionId));
    }
}
