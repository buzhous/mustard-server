package org.jeecg.modules.ai.port;

import com.alibaba.fastjson.JSONObject;

/**
 * 可观测埋点端口：ai 模块上报运行时事件（实现方在 app 模块，复用 tbl_stats_event 存储）。
 * fire-and-forget：实现方必须吞异常，埋点失败不影响主链路。
 */
public interface ObservabilityPort {

    /**
     * 上报事件
     *
     * @param event 事件名（如 ai_search_trace）
     * @param uid   用户（可空）
     * @param data  事件载荷
     */
    void fire(String event, String uid, JSONObject data);
}
