package org.jeecg.modules.app.openapi;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.app.entity.OpenAuditLog;
import org.jeecg.modules.app.mapper.OpenAuditLogMapper;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * 开放平台审计落库（设计方案 3.4 / E3）
 * <p>
 * 单线程异步落库，失败不阻塞主流程（仅记 warn 日志）；
 * App 授权页「调用记录」视图的数据源（用户可见"谁在何时读过我的物品"）。
 */
@Slf4j
@Component
public class OpenAuditLogger {

    private final OpenAuditLogMapper auditLogMapper;

    /** 审计专用单线程池（daemon，应用退出不阻塞） */
    private final ExecutorService auditExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "open-audit-logger");
        t.setDaemon(true);
        return t;
    });

    public OpenAuditLogger(OpenAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 异步记录一次开放调用（REST channel=rest / MCP channel=mcp，MCP path 记 tool 名）
     */
    public void log(String appKey, String boundUid, String channel, String path, String action,
                    String paramsDigest, Integer resultCode, Integer dataCount, Integer costMs, String clientIp) {
        OpenAuditLog auditLog = new OpenAuditLog()
                .setAppKey(appKey)
                .setBoundUid(boundUid == null ? "" : boundUid)
                .setChannel(channel)
                .setPath(path)
                .setAction(action)
                .setParamsDigest(truncate(paramsDigest))
                .setResultCode(resultCode)
                .setDataCount(dataCount)
                .setCostMs(costMs)
                .setClientIp(clientIp)
                .setCreateTime(new Date());
        try {
            auditExecutor.execute(() -> {
                try {
                    auditLogMapper.insert(auditLog);
                } catch (Exception e) {
                    log.warn("[open-audit] 审计落库失败 path={} appKey={}: {}", path, appKey, e.getMessage());
                }
            });
        } catch (RejectedExecutionException ignored) {
            // 队列已关闭（应用关闭中），放弃本条审计
        }
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
