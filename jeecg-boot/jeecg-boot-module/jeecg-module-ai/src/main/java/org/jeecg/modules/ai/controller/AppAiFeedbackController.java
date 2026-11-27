package org.jeecg.modules.ai.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.ai.bean.enums.AiErrorEnum;
import org.jeecg.modules.ai.port.AiAuthPort;
import org.jeecg.modules.ai.port.AiFeedbackPort;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * App 端 AI 反馈接口（P4-7）：查询卡/草稿卡 👍/👎 反馈提交。
 * 鉴权对齐 AppAiChatController（AiAuthPort.currentUid）；
 * 写入 tbl_ai_feedback source_type=1（用户反馈），uid+ref_content 去重合并更新。
 */
@Slf4j
@RestController
@RequestMapping("/app/ai/feedback")
@Tag(name = "App-AI反馈")
public class AppAiFeedbackController {

    /** 查询词/草稿摘要长度上限（ref_content 为 text，摘要口径收敛） */
    private static final int MAX_REF_CONTENT = 500;
    /** 结果摘要/原因长度上限（varchar 255） */
    private static final int MAX_SUMMARY = 255;

    private final AiAuthPort authPort;
    private final AiFeedbackPort feedbackPort;

    public AppAiFeedbackController(AiAuthPort authPort, AiFeedbackPort feedbackPort) {
        this.authPort = authPort;
        this.feedbackPort = feedbackPort;
    }

    @Operation(summary = "查询/草稿卡反馈提交（body: sessionId?, messageId?, refType(1查询/2草稿), refContent, resultSummary?, reason?）")
    @PostMapping("/feedback")
    public Result<String> feedback(@RequestBody JSONObject body) {
        String uid = authPort.currentUid();
        if (ObjectUtil.isEmpty(uid)) {
            return Result.error(AiErrorEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        Integer refType = body.getInteger("refType");
        String refContent = StrUtil.trimToNull(body.getString("refContent"));
        if (refType == null || (refType != 1 && refType != 2) || refContent == null) {
            return Result.error(AiErrorEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        feedbackPort.submitUserFeedback(uid,
                StrUtil.trimToNull(body.getString("sessionId")),
                StrUtil.trimToNull(body.getString("messageId")),
                refType,
                StrUtil.maxLength(refContent, MAX_REF_CONTENT),
                StrUtil.maxLength(StrUtil.trimToNull(body.getString("resultSummary")), MAX_SUMMARY),
                StrUtil.maxLength(StrUtil.trimToNull(body.getString("reason")), MAX_SUMMARY));
        return Result.OK("感谢反馈");
    }
}
