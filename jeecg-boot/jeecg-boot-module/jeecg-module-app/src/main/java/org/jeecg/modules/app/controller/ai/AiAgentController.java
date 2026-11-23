package org.jeecg.modules.app.controller.ai;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.vo.ai.AiChatCreateVO;
import org.jeecg.modules.app.bean.vo.ai.AiChatSendVO;
import org.jeecg.modules.app.entity.user.AppUser;
import org.jeecg.modules.app.service.AiAgentService;
import org.jeecg.modules.app.utils.AppAuthUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 建档编排端点（ai-integration-plan §5.9 S3/S4/S5）
 * send：发消息 → 解析 → 草稿 JSON + 置信度 / 追问；create：确认创建 → 数量 N → MCP item_create ×N。
 */
@Slf4j
@RestController
@RequestMapping("/app/ai/chat")
@Tag(name = "App-AI建档编排")
public class AiAgentController {

    private final AiAgentService agentService;

    public AiAgentController(AiAgentService agentService) {
        this.agentService = agentService;
    }

    @Operation(summary = "发送消息（解析→草稿/追问/闲聊）")
    @PostMapping("/send")
    public Result<AiChatSendVO> send(@RequestBody JSONObject body) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error(ExceptionEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        if (body == null || ObjectUtil.isEmpty(body.getString("sessionId"))) {
            return Result.error(ExceptionEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        List<String> images = body.getJSONArray("images") == null
                ? null : body.getJSONArray("images").toJavaList(String.class);
        return Result.OK(agentService.send(loginUser.getId(), body.getString("sessionId"),
                body.getString("text"), images));
    }

    @Operation(summary = "确认创建（数量 1-99，默认 1）")
    @PostMapping("/create")
    public Result<AiChatCreateVO> create(@RequestBody JSONObject body) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error(ExceptionEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        if (body == null || ObjectUtil.isEmpty(body.getString("sessionId"))) {
            return Result.error(ExceptionEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        // draft 兼容两种形态：App 传嵌套对象 / 外部传 JSON 字符串；
        // fastjson2 兼容层 getString 对嵌套对象返回 LinkedHashMap toString，需显式 toJSONString
        Object draftParam = body.get("draft");
        String draftJson = draftParam == null ? null
                : draftParam instanceof String ? (String) draftParam : JSON.toJSONString(draftParam);
        return Result.OK(agentService.confirmCreate(loginUser.getId(),
                body.getString("sessionId"), body.getInteger("quantity"), draftJson));
    }
}
