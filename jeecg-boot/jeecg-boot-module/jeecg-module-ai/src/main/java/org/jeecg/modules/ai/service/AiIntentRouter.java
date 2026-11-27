package org.jeecg.modules.ai.service;

import cn.hutool.core.util.StrUtil;
import org.jeecg.modules.ai.bean.vo.ai.AiChatSendVO;
import org.jeecg.modules.ai.stream.AiStreamSink;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 意图路由（方案 4.4）：正则 STRONG/WEAK_QUERY 保留为快路径；
 * 未命中正则时由 filing v2 解析同轮输出的 intent 三分类（file/query/chat）兜底，
 * 不单独调用模型（与解析合并为单次调用）。带图默认建档不变。
 */
public final class AiIntentRouter {

    public static final String INTENT_FILE = "file";
    public static final String INTENT_QUERY = "query";
    public static final String INTENT_CHAT = "chat";
    public static final String INTENT_UPDATE = "update";

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

    /** mock 快路径条件一：模拟/虚构类话术词 */
    private static final Pattern MOCK_HINT = Pattern.compile("模拟|虚构|假装|试想|帮我编|编个|编一个|虚拟");
    /** mock 快路径条件二：建档语义锚点（与条件一同时命中才判 mock） */
    private static final Pattern MOCK_ANCHOR = Pattern.compile("建档|档案|物品|收录|帮我记|一个|一下|买了|买过|入手");
    /** mock 排除词：模拟器/模拟考/模拟经营等非建档语境 */
    private static final Pattern MOCK_EXCLUDE = Pattern.compile("模拟(器|考|经营|量|测试|数据|环境|盘|信号|退火|点击)");

    /** update 强语型：带明确变更目标的动词（命中即走 update 管线，仅纯文本） */
    private static final Pattern STRONG_UPDATE = Pattern.compile(
            "改成|换成|改为|更新为|设为|置为|调为|调到|挪到|搬到|标记为|补成");
    /** update 弱语型：变更动词 + 物品字段锚点同时命中才判 update（防「修改密码」等误路由） */
    private static final Pattern WEAK_UPDATE = Pattern.compile("修改|更新|改一下|调整下|调整一下|补充下|补充一下|完善下|完善一下");
    /** update 字段锚点：可被自然语言编辑的物品信息语义 */
    private static final Pattern UPDATE_FIELD_ANCHOR = Pattern.compile(
            "价格|名字|名称|描述|备注|标签|成色|品牌|型号|序列号|等级|购入|购买时间|信息|资料");
    /** update 排除词：建档/模拟语境恒不走 update 管线 */
    private static final Pattern UPDATE_EXCLUDE = Pattern.compile("建档|收录|帮我记|记一笔|模拟|虚构|假装");

    /**
     * 本轮是否为模拟建档话术（正则快路径）：模拟/虚构类词与建档语义锚点同时命中，且未命中排除词
     * （模拟器/模拟考等）。命中后编排器按 mock 契约出稿；未命中时模型可经 prompt 内联 mock 语义兜底。
     */
    public static boolean isMockText(String text) {
        if (StrUtil.isBlank(text)) {
            return false;
        }
        String s = text.trim();
        if (MOCK_EXCLUDE.matcher(s).find()) {
            return false;
        }
        return MOCK_HINT.matcher(s).find() && MOCK_ANCHOR.matcher(s).find();
    }

    /**
     * 本轮是否为编辑已有物品话术（update 快路径，P5）：
     * 强语型（改成/换成/改为…）命中即判；弱语型（修改/更新/补充…）需与物品字段锚点同时命中；
     * 排除词（建档/模拟语境）恒不判。调用方保证纯文本 + 非 express/mock（带图恒为建档）。
     */
    public static boolean isUpdateText(String text) {
        if (StrUtil.isBlank(text)) {
            return false;
        }
        String s = text.trim();
        if (UPDATE_EXCLUDE.matcher(s).find()) {
            return false;
        }
        if (STRONG_UPDATE.matcher(s).find()) {
            return true;
        }
        return WEAK_UPDATE.matcher(s).find() && UPDATE_FIELD_ANCHOR.matcher(s).find();
    }

    /**
     * express 模式下 queryService.send 的包装（保持调用点类型一致）
     */
    public static AiChatSendVO delegateQuery(AiQueryService queryService, String uid, String sessionId,
                                             String text, List<String> images) {
        return queryService.send(uid, sessionId, text, images);
    }

    /**
     * 带流式事件汇的查询委托（SSE 通道）：sink 透传给查询管线，assistant 消息落库即推
     */
    public static AiChatSendVO delegateQuery(AiQueryService queryService, String uid, String sessionId,
                                             String text, List<String> images, AiStreamSink sink) {
        return queryService.send(uid, sessionId, text, images, sink);
    }
}
