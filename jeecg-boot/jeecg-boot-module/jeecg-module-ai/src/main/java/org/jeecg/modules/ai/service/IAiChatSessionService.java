package org.jeecg.modules.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.ai.entity.AiChatSession;

import java.util.List;

public interface IAiChatSessionService extends IService<AiChatSession> {

    /**
     * 创建新会话（新对话唯一入口）
     */
    AiChatSession createSession(String uid);

    /**
     * 会话列表（更新时间倒序，上限 50）
     */
    List<AiChatSession> listByUid(String uid);

    /**
     * 取归属校验后的会话（不存在或不属于当前用户返回 null）
     */
    AiChatSession getOwned(String uid, String sessionId);

    /**
     * 更新会话元信息（标题/状态/摘要/追问计数/草稿快照），触摸 update_time
     */
    void touch(AiChatSession session);
}
