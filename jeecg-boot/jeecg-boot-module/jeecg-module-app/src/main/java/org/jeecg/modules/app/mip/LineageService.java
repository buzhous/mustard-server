package org.jeecg.modules.app.mip;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.app.entity.ItemLineage;
import org.jeecg.modules.app.mapper.ItemLineageMapper;
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

    public LineageService(ItemLineageMapper lineageMapper) {
        this.lineageMapper = lineageMapper;
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
