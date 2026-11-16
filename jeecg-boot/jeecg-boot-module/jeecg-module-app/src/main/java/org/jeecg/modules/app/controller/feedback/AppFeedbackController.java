package org.jeecg.modules.app.controller.feedback;

import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.entity.feedback.Feedback;
import org.jeecg.modules.app.entity.user.AppUser;
import org.jeecg.modules.app.service.IAppFeedbackService;
import org.jeecg.modules.app.utils.AppAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;

/**
 * 用户反馈（App 提交入口，后台在 /feedback/admin 管理）
 */
@Slf4j
@RestController
@RequestMapping("/app/feedback")
@Tag(name = "App-用户反馈")
public class AppFeedbackController {

    /**
     * 反馈内容最大长度
     */
    private static final int CONTENT_MAX_LEN = 500;

    /**
     * 联系方式最大长度
     */
    private static final int CONTACT_MAX_LEN = 64;

    @Autowired
    private IAppFeedbackService feedbackService;

    @Operation(summary = "提交反馈（content 必填 ≤500 字，contact 选填 ≤64 字）")
    @PostMapping("/submit")
    public Result<String> submit(@RequestBody Map<String, String> body) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (loginUser == null || StrUtil.isEmpty(loginUser.getId())) {
            return Result.error(ExceptionEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        String content = body == null ? null : body.get("content");
        String contact = body == null ? null : body.get("contact");
        if (StrUtil.isBlank(content)) {
            return Result.error("请填写反馈内容");
        }
        content = content.trim();
        if (content.length() > CONTENT_MAX_LEN) {
            return Result.error("反馈内容不能超过 " + CONTENT_MAX_LEN + " 字");
        }
        if (StrUtil.isNotBlank(contact) && contact.trim().length() > CONTACT_MAX_LEN) {
            return Result.error("联系方式不能超过 " + CONTACT_MAX_LEN + " 字");
        }
        Feedback feedback = new Feedback()
                .setUserId(loginUser.getId())
                .setContent(content)
                .setContact(StrUtil.isBlank(contact) ? "" : contact.trim())
                .setStatus(Feedback.STATUS_PENDING)
                .setCreateTime(new Date())
                .setUpdateTime(new Date());
        feedbackService.save(feedback);
        return Result.OK("提交成功，感谢您的反馈");
    }
}
