package org.jeecg.modules.app.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.app.entity.KuaidiOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 快递100 mock 模式状态自动推进调度
 * 仅在 mock（未配置真实密钥）模式启用：下单后按拟真节奏自动推进
 * 揽收(2)→运输(3)→签收(5)，与真实回调共用 updateOrderStatus/onKuaidiStatus 处理链；
 * 配置真实密钥后本调度不再触发（createOrder 仅 mock 分支调用），由快递100 服务器真实回调接管
 *
 * @author elinx
 */
@Slf4j
@Component
public class KuaidiMockScheduler {

    /** @Lazy 破解 Kuaidi100Service→Scheduler→StorageIn→Kuaidi100Service 循环依赖 */
    @Autowired
    @Lazy
    private Kuaidi100Service kuaidi100Service;

    @Autowired
    @Lazy
    private IItemStorageInService itemStorageInService;

    @Value("${jeecg.kuaidi100.mock-pickup-seconds:30}")
    private int pickupSeconds;

    @Value("${jeecg.kuaidi100.mock-transit-seconds:75}")
    private int transitSeconds;

    @Value("${jeecg.kuaidi100.mock-signed-seconds:150}")
    private int signedSeconds;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "kuaidi-mock");
        t.setDaemon(true);
        return t;
    });

    /**
     * mock 下单成功后调度拟真状态流转：揽收→运输→签收（节奏可配，秒）
     * 服务重启会丢失未执行的调度任务，mock 模式可接受（可经 /app/kuaidi/callback 手动补推）
     */
    public void scheduleMockFlow(KuaidiOrder order) {
        if (order == null || StrUtil.isBlank(order.getTaskId())) {
            return;
        }
        log.info("快递100 mock 流转调度 taskId={}, 揽收{}s/运输{}s/签收{}s",
                order.getTaskId(), pickupSeconds, transitSeconds, signedSeconds);
        executor.schedule(() -> advance(order.getTaskId(), 2),
                Math.max(1, pickupSeconds), TimeUnit.SECONDS);
        executor.schedule(() -> advance(order.getTaskId(), 3),
                Math.max(2, transitSeconds), TimeUnit.SECONDS);
        executor.schedule(() -> advance(order.getTaskId(), 5),
                Math.max(3, signedSeconds), TimeUnit.SECONDS);
    }

    /**
     * 状态递进（只进不退，避免覆盖手动回调已推进的状态），与真实回调同一条处理链
     */
    private void advance(String taskId, int status) {
        try {
            KuaidiOrder latest = kuaidi100Service.getByTaskId(taskId);
            if (latest == null) {
                return;
            }
            if (latest.getOrderStatus() >= status) {
                return;
            }
            kuaidi100Service.updateOrderStatus(latest, status, null,
                    "{\"mock\":true,\"status\":" + status + "}");
            itemStorageInService.onKuaidiStatus(latest, status);
            log.info("快递100 mock 状态推进 taskId={}, status={}", taskId, status);
        } catch (Exception e) {
            log.error("快递100 mock 状态推进失败 taskId={}, status={}", taskId, status, e);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
