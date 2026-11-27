package org.jeecg.modules.app.aiport;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.ai.port.AiFeedbackPort;
import org.jeecg.modules.app.entity.AiFeedback;
import org.jeecg.modules.app.mapper.AiFeedbackMapper;

import java.util.Date;

/**
 * AiFeedbackPort 桥接实现：App 用户反馈写入 tbl_ai_feedback（source_type=1），
 * 同 uid+ref_content+source_type 已存在则合并更新（session/message/摘要/原因/时间），不新增行。
 */
public class AiFeedbackPortImpl implements AiFeedbackPort {

    /** 用户反馈来源类型（tbl_ai_feedback.source_type：1用户反馈） */
    private static final int SOURCE_TYPE_USER = 1;

    private final AiFeedbackMapper feedbackMapper;

    public AiFeedbackPortImpl(AiFeedbackMapper feedbackMapper) {
        this.feedbackMapper = feedbackMapper;
    }

    @Override
    public void submitUserFeedback(String uid, String sessionId, String messageId, Integer refType,
                                   String refContent, String resultSummary, String reason) {
        Date now = new Date();
        AiFeedback exist = feedbackMapper.selectOne(new LambdaQueryWrapper<AiFeedback>()
                .eq(AiFeedback::getUid, uid)
                .eq(AiFeedback::getSourceType, SOURCE_TYPE_USER)
                .eq(AiFeedback::getRefContent, refContent)
                .last("LIMIT 1"));
        if (exist != null) {
            AiFeedback update = new AiFeedback()
                    .setId(exist.getId())
                    .setSessionId(sessionId)
                    .setMessageId(messageId)
                    .setRefType(refType)
                    .setResultSummary(resultSummary)
                    .setReason(reason)
                    .setCreateTime(now);
            feedbackMapper.updateById(update);
            return;
        }
        feedbackMapper.insert(new AiFeedback()
                .setSourceType(SOURCE_TYPE_USER)
                .setSessionId(sessionId)
                .setMessageId(messageId)
                .setUid(uid)
                .setRefType(refType)
                .setRefContent(refContent)
                .setResultSummary(resultSummary)
                .setReason(reason)
                .setSampleStatus(0)
                .setCreateTime(now));
    }
}
