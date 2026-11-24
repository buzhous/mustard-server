package org.jeecg.modules.ai.service;

import cn.hutool.core.util.StrUtil;
import org.jeecg.modules.ai.bean.vo.ai.AiChatSendVO;

import java.util.List;

/**
 * 意图路由（方案 4.4）：正则 STRONG/WEAK_QUERY 保留为快路径；
 * 未命中正则时由 filing v2 解析同轮输出的 intent 三分类（file/query/chat）兜底，
 * 不单独调用模型（与解析合并为单次调用）。带图默认建档不变。
 */
public final class AiIntentRouter {

    public static final String INTENT_FILE = "file";
    public static final String INTENT_QUERY = "query";
    public static final String INTENT_CHAT = "chat";

    private AiIntentRouter() {
    }

    /**
     * 本轮是否应转交查询管线（C2）。
     *
     * @param text            用户输入
     * @param images          附图（带图恒为建档）
     * @param decisionIntent  filing v2 解析同轮给出的 intent（可空）
     * @param expressMode     express 模式恒为建档
     */
    public static boolean routeToQuery(String text, List<String> images, String decisionIntent, boolean expressMode) {
        if (expressMode) {
            return false;
        }
        // 快路径：强/弱语型正则（带图仅强语型生效）
        if (AiQueryService.isQueryText(text, images)) {
            return true;
        }
        // 模型兜底：仅纯文本 + 显式 query 意图
        return (images == null || images.isEmpty())
                && INTENT_QUERY.equals(StrUtil.trimToNull(decisionIntent));
    }

    /**
     * express 模式下 queryService.send 的包装（保持调用点类型一致）
     */
    public static AiChatSendVO delegateQuery(AiQueryService queryService, String uid, String sessionId,
                                             String text, List<String> images) {
        return queryService.send(uid, sessionId, text, images);
    }
}
