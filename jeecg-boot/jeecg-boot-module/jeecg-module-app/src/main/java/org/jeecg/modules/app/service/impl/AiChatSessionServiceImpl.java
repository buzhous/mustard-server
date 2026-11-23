package org.jeecg.modules.app.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.entity.AiChatSession;
import org.jeecg.modules.app.mapper.AiChatSessionMapper;
import org.jeecg.modules.app.service.IAiChatSessionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiChatSessionServiceImpl
        extends ServiceImpl<AiChatSessionMapper, AiChatSession>
        implements IAiChatSessionService {

    private static final int MAX_SESSIONS = 50;

    @Override
    public AiChatSession createSession(String uid) {
        if (StrUtil.isEmpty(uid)) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        AiChatSession session = new AiChatSession()
                .setUid(uid)
                .setTitle("新对话")
                .setStatus(AiChatSession.STATUS_ACTIVE)
                .setLastSummary("")
                .setAskCount(0)
                .setDraftSnapshot(null);
        this.save(session);
        return session;
    }

    @Override
    public List<AiChatSession> listByUid(String uid) {
        if (StrUtil.isEmpty(uid)) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        return this.list(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUid, uid)
                .orderByDesc(AiChatSession::getUpdateTime)
                .last("limit " + MAX_SESSIONS));
    }

    @Override
    public AiChatSession getOwned(String uid, String sessionId) {
        if (StrUtil.hasEmpty(uid, sessionId)) {
            return null;
        }
        AiChatSession session = this.getById(sessionId);
        if (session == null || !uid.equals(session.getUid())) {
            return null;
        }
        return session;
    }

    @Override
    public void touch(AiChatSession session) {
        this.updateById(session);
    }
}
