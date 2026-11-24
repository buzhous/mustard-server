package org.jeecg.modules.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.ai.bean.enums.AiErrorEnum;
import org.jeecg.modules.ai.bean.exception.AppException;
import org.jeecg.modules.ai.entity.AiChatMessage;
import org.jeecg.modules.ai.mapper.AiChatMessageMapper;
import org.jeecg.modules.ai.service.IAiChatMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiChatMessageServiceImpl
        extends ServiceImpl<AiChatMessageMapper, AiChatMessage>
        implements IAiChatMessageService {

    private static final int MAX_MESSAGES = 200;

    @Override
    public AiChatMessage append(AiChatMessage message) {
        if (message == null || StrUtil.hasEmpty(message.getSessionId(), message.getUid(), message.getRole())) {
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR);
        }
        this.save(message);
        return message;
    }

    @Override
    public List<AiChatMessage> listBySession(String uid, String sessionId) {
        if (StrUtil.hasEmpty(uid, sessionId)) {
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR);
        }
        return this.list(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId)
                .eq(AiChatMessage::getUid, uid)
                .orderByAsc(AiChatMessage::getCreateTime)
                .last("limit " + MAX_MESSAGES));
    }
}
