package org.jeecg.modules.ai.stream;

import org.jeecg.modules.ai.bean.vo.ai.AiChatSendVO;
import org.jeecg.modules.ai.entity.AiChatMessage;

/**
 * AI 聊天流式事件汇（L1 阶段流，方案见 docs/ai-sse-infrastructure-evaluation.md §三）。
 * <p>
 * 事件契约：session / progress / message / done / error（done = 完整 AiChatSendVO，与旧 POST
 * Result.data 同构；error 为唯一终端事件的替代出口）。
 * <p>
 * 契约预留（本期不实现，仅留位防契约破坏）：delta（token/字段增量，L2 流式演进）、
 * surface（A2UI 消息下发）。断连后实现方内部 no-op，业务任务继续执行并照常落库（断连不回放）。
 */
public interface AiStreamSink {

    /** session 立即回执事件：限流/校验通过、任务即将启动（data 仅含 sessionId，其余状态随 done 下发） */
    void session(String sessionId);

    /** 管线里程碑事件：stage = vision（视觉识别）| parsing（意图解析） */
    void progress(String stage);

    /** 单条 assistant 消息落库即推（用户消息不推，App 端有乐观气泡） */
    void message(AiChatMessage msg);

    /** 终端事件：完整 VO（与旧 POST Result.data 同构），发送后 complete */
    void done(AiChatSendVO vo);

    /** 终端事件替代出口：任何失败（含限流/参数/模型异常），发送后 complete */
    void error(int code, String message);
}
