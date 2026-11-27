package org.jeecg.modules.app.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.app.entity.StatsEvent;
import org.jeecg.modules.app.mapper.StatsEventMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * F1 轻量自研采集统计 · 事件服务（设计方案 2.1）
 * <p>
 * 事件字典（8 基线）：app_launch / register / first_item_created / coupon_claimed /
 * pickup_claimed / gift_sent / appkey_activated / payment_success。
 * <p>
 * 约束：
 * - fire() 供服务端业务挂点调用，失败只 warn 不抛出（绝不影响业务主流程）；
 * - batchReport() 供 App 端 /app/stats/track/batch 上报，eventId 幂等（先查存在再插入）；
 * - 明确不采集 PII（物品名称/描述/图片、手机号等），props 仅白名单键。
 */
@Slf4j
@Service
public class StatsEventService {

    /** 事件白名单（8 基线事件 + P2-7 兜底链观测事件 + P3 会话回放审计） */
    public static final Set<String> ALLOWED_EVENTS = Set.of(
            "app_launch", "register", "first_item_created", "coupon_claimed",
            "pickup_claimed", "gift_sent", "appkey_activated", "payment_success",
            "ai_search_trace", "ai_session_viewed");

    /** 允许匿名上报的事件（未登录） */
    public static final Set<String> ANONYMOUS_EVENTS = Set.of("app_launch");

    /** 单批上限 */
    public static final int MAX_BATCH = 50;

    private final StatsEventMapper eventMapper;

    public StatsEventService(StatsEventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }

    /**
     * 服务端业务挂点埋点（fire-and-forget，吞异常）
     *
     * @param event 事件名（白名单内）
     * @param uid   用户 id（可空）
     * @param props 扩展属性（JSON，键白名单由调用方保证最小化）
     */
    public void fire(String event, String uid, JSONObject props) {
        try {
            if (!ALLOWED_EVENTS.contains(event)) {
                log.warn("[stats] 非法事件名，忽略: {}", event);
                return;
            }
            StatsEvent e = new StatsEvent()
                    .setEventId(java.util.UUID.randomUUID().toString())
                    .setEvent(event)
                    .setUid(StrUtil.nullToEmpty(uid))
                    .setServerTs(System.currentTimeMillis())
                    .setClientTs(System.currentTimeMillis())
                    .setStatsDate(today());
            e.setProps(props == null ? "{}" : props.toJSONString());
            eventMapper.insert(e);
        } catch (Exception ex) {
            log.warn("[stats] 事件埋点失败（不影响业务）event={} uid={}: {}", event, uid, ex.getMessage());
        }
    }

    /**
     * App 批量上报入库（eventId 幂等：已存在的跳过）
     *
     * @return 实际入库的 eventId 集合
     */
    public Set<String> batchReport(List<StatsEvent> events) {
        if (events == null || events.isEmpty()) {
            return Set.of();
        }
        List<String> ids = events.stream().map(StatsEvent::getEventId).toList();
        Set<String> existed = new HashSet<>();
        if (!ids.isEmpty()) {
            LambdaQueryWrapper<StatsEvent> qw = new LambdaQueryWrapper<StatsEvent>()
                    .select(StatsEvent::getEventId).in(StatsEvent::getEventId, ids);
            eventMapper.selectList(qw).forEach(e -> existed.add(e.getEventId()));
        }
        List<StatsEvent> toInsert = new ArrayList<>();
        for (StatsEvent e : events) {
            if (existed.contains(e.getEventId())) {
                continue;
            }
            if (e.getServerTs() == null) {
                e.setServerTs(System.currentTimeMillis());
            }
            if (e.getStatsDate() == null) {
                e.setStatsDate(fromClientTs(e.getClientTs()));
            }
            toInsert.add(e);
        }
        for (StatsEvent e : toInsert) {
            try {
                eventMapper.insert(e);
            } catch (Exception ex) {
                log.warn("[stats] 上报事件入库失败 eventId={}: {}", e.getEventId(), ex.getMessage());
            }
        }
        return toInsert.stream().map(StatsEvent::getEventId).collect(java.util.stream.Collectors.toSet());
    }

    /** 服务端挂点：归属当日 */
    private Date today() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /** App 上报：按客户端时间戳归属自然日（服务端时区）；时钟偏差 >48h 以服务端时间修正（48h 容忍跨日测试与慢时钟设备） */
    private Date fromClientTs(Long clientTs) {
        long ts = clientTs == null ? System.currentTimeMillis() : clientTs;
        long now = System.currentTimeMillis();
        if (Math.abs(now - ts) > 48L * 3600 * 1000) {
            ts = now;
        }
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
