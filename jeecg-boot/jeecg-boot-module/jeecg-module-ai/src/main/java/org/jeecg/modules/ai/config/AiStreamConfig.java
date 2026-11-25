package org.jeecg.modules.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 流式通道线程基建（L1 SSE）：
 * 有界专用线程池跑流式任务（不占 Tomcat worker，AbortPolicy 快速拒绝），
 * 单线程调度器发 SSE 心跳 comment 帧。
 */
@Configuration
public class AiStreamConfig {

    /** 流式任务线程池：core 2 / max 8 / 队列 32 / 拒绝抛 RejectedExecutionException 由调用方补 error 事件 */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService aiStreamExecutor() {
        AtomicInteger seq = new AtomicInteger();
        return new ThreadPoolExecutor(2, 8, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(32),
                r -> {
                    Thread t = new Thread(r, "ai-stream-" + seq.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }

    /** SSE 心跳调度器：单守护线程 */
    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService aiStreamHeartbeat() {
        ScheduledExecutorService scheduler = new java.util.concurrent.ScheduledThreadPoolExecutor(
                1, r -> {
                    Thread t = new Thread(r, "ai-stream-heartbeat");
                    t.setDaemon(true);
                    return t;
                });
        return scheduler;
    }
}
