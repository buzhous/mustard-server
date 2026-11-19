package org.jeecg.modules.app.job;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.app.service.IItemRecycleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 回收站超期清理：删除 30 天前未恢复的用户独立回收站记录（彻底删除），
 * 每天凌晨 3 点执行
 */
@Slf4j
@Component
public class ItemRuinsCleanupJob {

    @Autowired
    private IItemRecycleService itemRecycleService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredRecycle() {
        try {
            int count = itemRecycleService.purgeExpiredRecycle();
            if (count > 0) {
                log.info("[回收站清理] 本次清理超期记录 {} 条", count);
            }
        } catch (Exception e) {
            log.error("[回收站清理] 执行失败", e);
        }
    }

}
