package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.entity.AiChatMessage;

import java.util.List;

public interface IAiChatMessageService extends IService<AiChatMessage> {

    /**
     * 追加消息（会话元信息摘要/触摸由调用方负责）
     */
    AiChatMessage append(AiChatMessage message);

    /**
     * 恢复消息流（创建时间正序，上限 200）
     */
    List<AiChatMessage> listBySession(String uid, String sessionId);
}
