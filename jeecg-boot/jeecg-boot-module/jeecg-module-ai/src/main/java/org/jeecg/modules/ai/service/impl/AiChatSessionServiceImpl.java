package org.jeecg.modules.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.ai.bean.enums.AiErrorEnum;
import org.jeecg.modules.ai.bean.exception.AppException;
import org.jeecg.modules.ai.entity.AiChatMessage;
import org.jeecg.modules.ai.entity.AiChatSession;
import org.jeecg.modules.ai.mapper.AiChatMessageMapper;
import org.jeecg.modules.ai.mapper.AiChatSessionMapper;
import org.jeecg.modules.ai.service.IAiChatSessionService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AiChatSessionServiceImpl
        extends ServiceImpl<AiChatSessionMapper, AiChatSession>
        implements IAiChatSessionService {

    private static final int MAX_SESSIONS = 50;

    private final AiChatMessageMapper messageMapper;

    public AiChatSessionServiceImpl(AiChatMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public AiChatSession createSession(String uid) {
        if (StrUtil.isEmpty(uid)) {
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR);
        }
        // 空会话复用：该用户已有「无任何消息」的活跃会话时不重复创建，
        // 沿用该会话并刷新时间戳排到列表最前（防止连点新建产生多个空会话）
        List<AiChatSession> sessions = this.list(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUid, uid)
                .orderByDesc(AiChatSession::getUpdateTime)
                .last("limit " + MAX_SESSIONS));
        Set<String> nonEmptyIds = new HashSet<>();
        if (!sessions.isEmpty()) {
            List<String> ids = sessions.stream().map(AiChatSession::getId).toList();
            messageMapper.selectObjs(new QueryWrapper<AiChatMessage>()
                    .select("DISTINCT session_id")
                    .in("session_id", ids))
                    .forEach(row -> nonEmptyIds.add(String.valueOf(row)));
        }
        for (AiChatSession session : sessions) {
            if (!nonEmptyIds.contains(session.getId())) {
                session.setUpdateTime(new Date());
                this.updateById(session);
                return session;
            }
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
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR);
        }
        return this.list(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUid, uid)
                .orderByDesc(AiChatSession::getPinned)
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

    @Override
    public AiChatSession rename(String uid, String sessionId, String title) {
        AiChatSession session = getOwned(uid, sessionId);
        if (session == null) {
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR);
        }
        String name = StrUtil.trimToEmpty(title);
        if (StrUtil.isEmpty(name)) {
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR);
        }
        if (name.length() > 50) {
            name = name.substring(0, 50);
        }
        session.setTitle(name);
        this.updateById(session);
        return session;
    }

    @Override
    public AiChatSession setPinned(String uid, String sessionId, boolean pinned) {
        AiChatSession session = getOwned(uid, sessionId);
        if (session == null) {
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR);
        }
        session.setPinned(pinned ? 1 : 0);
        this.updateById(session);
        return session;
    }

    @Override
    public void softDelete(String uid, String sessionId) {
        AiChatSession session = getOwned(uid, sessionId);
        if (session == null) {
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR);
        }
        // 软删：del_flag=1（@TableLogic 自动转 update）；消息表不清理，
        // 会话入口（getOwned/listByUid）已过滤，消息随之不可见
        this.removeById(sessionId);
    }
}
