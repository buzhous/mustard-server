package org.jeecg.modules.app.mip;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.app.entity.ItemLineage;
import org.jeecg.modules.app.mapper.ItemLineageMapper;
import org.jeecg.modules.app.openapi.webhook.OpenWebhookHook;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * MIP v1 流转账本服务（MIP 协议 5 章 / 10.3 缺陷①②）
 * <p>
 * - append-only 哈希成链：hash = sha256(prev_hash + canonical(事件体))；
 * - <strong>与业务 Service 同事务</strong>（REQUIRED 传播）：业务落库与记账原子提交；
 * - 幂等防重：idempotency_key 唯一约束（ori_id+event_type+业务单号），
 *   同步队列重试场景不产生重复事件（Gate-G2）；
 * - restore 不记账；destroy 后该 ori_id 终链（读端校验，写入端不做状态机）。
 */
@Service
public class LineageService {

    /** 操作者类别 */
    public static final String OPERATOR_USER = "user";
    public static final String OPERATOR_SYSTEM = "system";
    public static final String OPERATOR_AGENT_PREFIX = "agent:";

    private final ItemLineageMapper lineageMapper;
    private final OpenWebhookHook webhookHook;

    public LineageService(ItemLineageMapper lineageMapper, OpenWebhookHook webhookHook) {
        this.lineageMapper = lineageMapper;
        this.webhookHook = webhookHook;
    }

    /**
     * 追加账本事件（同事务、幂等、自动成链）
     * <p>
     * 调用方只需填充：oriId/eventType/fromUid/toUid/fromStation/toStation/operator/payload/qty/parentOri/idempotencyKey，
     * uid 建议先经 {@link #hashUid} 哈希；occurredAt/prevHash/hash 由本方法填充。
     *
     * @return 落库后的事件（幂等命中时返回既有事件）
     */
    @Transactional(rollbackFor = Exception.class)
    public ItemLineage append(ItemLineage draft) {
        if (draft == null || draft.getOriId() == null || draft.getOriId().isBlank()
                || draft.getEventType() == null || draft.getEventType().isBlank()) {
            throw new IllegalArgumentException("lineage 事件缺少 oriId 或 eventType");
        }
        // 幂等命中：同步队列重试直接返回既有事件，不追加（防哈希链污染）
        if (draft.getIdempotencyKey() != null && !draft.getIdempotencyKey().isBlank()) {
            ItemLineage existing = selectByIdempotencyKey(draft.getIdempotencyKey());
            if (existing != null) {
                return existing;
            }
        }
        if (draft.getOccurredAt() == null) {
            draft.setOccurredAt(new Date());
        }
        // 时间归一到秒：tbl_item_lineage.occurred_at 为 DATETIME（秒精度），毫秒落库即截断，
        // 若 hash 覆盖毫秒值则 verify 重算（读 DB 值）必然失配 → 统一截断到秒后再入哈希
        draft.setOccurredAt(new Date((draft.getOccurredAt().getTime() / 1000) * 1000));
        // 取链尾：同 ori_id 最新一条事件
        ItemLineage last = lineageMapper.selectOne(new LambdaQueryWrapper<ItemLineage>()
                .eq(ItemLineage::getOriId, draft.getOriId())
                .orderByDesc(ItemLineage::getId)
                .last("limit 1"));
        String prevHash = last == null ? MipHashUtil.GENESIS_PREV_HASH : last.getHash();
        draft.setPrevHash(prevHash);
        draft.setHash(MipHashUtil.eventHash(prevHash, eventBody(draft)));
        try {
            lineageMapper.insert(draft);
            // B4 Webhook：命中订阅者则入队（与业务同事务，异常不阻断业务与记账）
            webhookHook.onAppended(draft);
            return draft;
        } catch (DuplicateKeyException e) {
            // 并发兜底：唯一约束拦截后返回既有事件
            ItemLineage existing = selectByIdempotencyKey(draft.getIdempotencyKey());
            if (existing != null) {
                return existing;
            }
            throw e;
        }
    }

    /**
     * 按 oriId 拉取完整事件链（读端复算验证用，按发生顺序）
     */
    public java.util.List<ItemLineage> chain(String oriId) {
        return lineageMapper.selectList(new LambdaQueryWrapper<ItemLineage>()
                .eq(ItemLineage::getOriId, oriId)
                .orderByAsc(ItemLineage::getId));
    }

    /**
     * 账本摘要（MIP 实例 lineage_digest 段）：event_count + head_hash
     */
    public JSONObject digest(String oriId) {
        java.util.List<ItemLineage> chain = chain(oriId);
        JSONObject d = new JSONObject();
        d.put("event_count", chain.size());
        d.put("head_hash", chain.isEmpty() ? null : chain.get(chain.size() - 1).getHash());
        return d;
    }

    /**
     * 链完整性校验 + 事件出域视图（开放平台 /app/open/item/lineage，设计方案 4.3）：
     * 逐事件重算 MipHashUtil.eventHash(prevHash, eventBody) 比对存储 hash，并校验链式 prevHash 衔接。
     * events 仅含出域最小字段集（opType/operator/prevHash/hash/occurredAt/qty），uid 均已哈希。
     */
    public JSONObject verify(String oriId) {
        java.util.List<ItemLineage> chain = chain(oriId);
        boolean chainValid = true;
        String prevHash = MipHashUtil.GENESIS_PREV_HASH;
        JSONArray events = new JSONArray();
        for (ItemLineage e : chain) {
            if (!matchesStoredHash(e) || !prevHash.equals(e.getPrevHash())) {
                chainValid = false;
            }
            JSONObject ev = new JSONObject(true);
            ev.put("opType", e.getEventType());
            ev.put("operator", e.getOperator());
            ev.put("prevHash", e.getPrevHash());
            ev.put("hash", e.getHash());
            ev.put("occurredAt", e.getOccurredAt() == null ? null : e.getOccurredAt().getTime());
            ev.put("qty", e.getQty());
            events.add(ev);
            prevHash = e.getHash();
        }
        JSONObject out = new JSONObject(true);
        out.put("chainValid", chainValid);
        out.put("event_count", chain.size());
        out.put("head_hash", chain.isEmpty() ? null : chain.get(chain.size() - 1).getHash());
        out.put("events", events);
        return out;
    }

    /**
     * uid 出链哈希：账本永不存明文 uid（MIP 协议 5.3 隐私规则）
     */
    public static String hashUid(String uid) {
        if (uid == null || uid.isBlank()) {
            return null;
        }
        return MipHashUtil.sha256Hex("uid:" + uid);
    }

    /**
     * agent 操作者标识：agent:{appKey尾4位}
     */
    public static String operatorAgent(String appKey) {
        if (appKey == null || appKey.length() < 4) {
            return OPERATOR_AGENT_PREFIX + "????";
        }
        return OPERATOR_AGENT_PREFIX + appKey.substring(appKey.length() - 4);
    }

    // ================================ 私有方法 ================================

    /**
     * 单事件哈希重算（verify 用）：标准（DB 秒精度值）不中时，遍历 0-999ms 变体兼容
     * 修复前存量事件（append 时 hash 覆盖了带毫秒值、落库被 DATETIME 截断）
     */
    private boolean matchesStoredHash(ItemLineage e) {
        if (e.getHash() == null) {
            return false;
        }
        if (MipHashUtil.eventHash(e.getPrevHash(), eventBody(e)).equals(e.getHash())) {
            return true;
        }
        if (e.getOccurredAt() != null && e.getOccurredAt().getTime() % 1000 == 0) {
            // DB 值毫秒已为 0：仅尝试存量原始毫秒变体
            long base = e.getOccurredAt().getTime();
            for (int ms = 1; ms <= 999; ms++) {
                e.setOccurredAt(new Date(base + ms));
                if (MipHashUtil.eventHash(e.getPrevHash(), eventBody(e)).equals(e.getHash())) {
                    e.setOccurredAt(new Date(base));
                    return true;
                }
            }
            e.setOccurredAt(new Date(base));
        }
        return false;
    }

    private ItemLineage selectByIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return lineageMapper.selectOne(new LambdaQueryWrapper<ItemLineage>()
                .eq(ItemLineage::getIdempotencyKey, key)
                .last("limit 1"));
    }

    /**
     * 事件体规范化（参与哈希的字段集合）；payload 为摘要 JSON，禁止属性值
     */
    private JSONObject eventBody(ItemLineage e) {
        JSONObject body = new JSONObject(true);
        body.put("ori_id", e.getOriId());
        body.put("event_type", e.getEventType());
        body.put("from_uid", e.getFromUid());
        body.put("to_uid", e.getToUid());
        body.put("from_station", e.getFromStation());
        body.put("to_station", e.getToStation());
        body.put("operator", e.getOperator());
        body.put("qty", e.getQty());
        body.put("parent_ori", e.getParentOri());
        body.put("occurred_at", e.getOccurredAt() == null ? null : e.getOccurredAt().getTime());
        body.put("payload", e.getPayload() == null ? null : JSON.parse(e.getPayload()));
        return body;
    }
}
