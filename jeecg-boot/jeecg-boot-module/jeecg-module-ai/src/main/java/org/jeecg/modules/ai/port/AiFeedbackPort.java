package org.jeecg.modules.ai.port;

/**
 * 反馈端口：App 端查询/草稿卡用户反馈写入 tbl_ai_feedback（app 模块桥接实现）。
 * 口径：source_type=1（用户反馈），同 uid+ref_content+source_type 去重合并更新。
 */
public interface AiFeedbackPort {

    /**
     * 提交用户反馈（失败抛异常由端点返回错误）
     *
     * @param uid           用户 uid
     * @param sessionId     会话 id（可空）
     * @param messageId     消息 id（可空）
     * @param refType       关联对象:1查询,2草稿
     * @param refContent    查询词或草稿摘要
     * @param resultSummary 结果摘要（可空）
     * @param reason        反馈原因（可空）
     */
    void submitUserFeedback(String uid, String sessionId, String messageId, Integer refType,
                            String refContent, String resultSummary, String reason);
}
