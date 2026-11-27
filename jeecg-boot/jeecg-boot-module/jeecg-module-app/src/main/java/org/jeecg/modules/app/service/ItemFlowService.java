package org.jeecg.modules.app.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.app.bean.enums.SyncStatusEnum;
import org.jeecg.modules.app.constant.StorageInConstant;
import org.jeecg.modules.app.entity.ItemInfo;
import org.jeecg.modules.app.entity.ItemLineage;
import org.jeecg.modules.app.entity.ItemStorageIn;
import org.jeecg.modules.app.entity.ItemSync;
import org.jeecg.modules.app.entity.ItemUserInventory;
import org.jeecg.modules.app.entity.OpenAppCredential;
import org.jeecg.modules.app.entity.user.AppUser;
import org.jeecg.modules.app.mapper.ItemStorageInMapper;
import org.jeecg.modules.app.mip.LineageService;
import org.jeecg.modules.app.mip.MipComponentRegistry;
import org.jeecg.modules.app.mip.MipEventType;
import org.jeecg.modules.app.mip.MipProfileBuilder;
import org.jeecg.modules.app.service.IItemInfoService;
import org.jeecg.modules.app.service.IItemSyncService;
import org.jeecg.modules.app.service.IItemUserInventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 定向流转内核（D1 两阶段 offer/accept，全程无资金结算、不做撮合抽佣）
 * <p>
 * 模型（方案 A，产品拍板）：
 * <ul>
 *   <li>两阶段：发起方 offer 生成 flowToken（Redis TTL 72h，绑定接收方）→ 接收方 accept/reject，
 *       发起方可 cancel；</li>
 *   <li>接收方范围：仅 App 端用户（按账号ID定位）；第三方 Agent 只能代发起（凭据绑定用户为发起方），
 *       仅接收方本人（凭据绑定用户 = 要约接收方）可 accept/reject，不支持代收；</li>
 *   <li>窗口期：72h 未响应自动失效（Redis TTL 兜底，无定时任务）；reject/cancel/超时仅清 token 与锁，
 *       不记账（custody 未变）；accept 落 flow_out/flow_in 事件对（同一哈希链）；</li>
 *   <li>不支持流转留言（无内容审核面）；</li>
 *   <li>物品锁：open:flow:lock:{itemId} 锁定期内拒 gift/delete/update/二次 offer（开放出口复核）。</li>
 * </ul>
 * 鉴权/scope/限流/审计由出口层完成，本类只做业务语义。
 */
@Slf4j
@Service
public class ItemFlowService {

    /** flowToken Redis 前缀（TTL 72h，绑定发起方+接收方，一次性） */
    public static final String KEY_FLOW_TOKEN = "open:flow:";
    /** 物品流转锁 Redis 前缀（锁定期拒 gift/delete/update/二次 offer） */
    public static final String KEY_FLOW_LOCK = "open:flow:lock:";
    /** 要约窗口：72h 未响应自动失效 */
    public static final long FLOW_WINDOW_SECONDS = 72 * 3600L;
    /** 幂等键 TTL（与平台写接口约定一致 24h） */
    private static final long IDEM_TTL_SECONDS = 86400;

    @Autowired
    private IItemInfoService itemInfoService;

    @Autowired
    private IItemUserInventoryService itemUserInventoryService;

    @Autowired
    private IItemSyncService itemSyncService;

    @Autowired
    private IAppUserService appUserService;

    @Autowired
    private ItemStorageInMapper itemStorageInMapper;

    @Autowired
    private LineageService lineageService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private org.jeecg.modules.app.service.ItemSearchIndexService searchIndexService;

    // ================================ 写内核 ================================

    /**
     * 发起流转要约：归属校验（库存）→ 接收方为平台用户 → 在途仓单红线 → 流转锁 → sensitivity 预检
     * → 签发 flowToken（绑定接收方，TTL 72h）+ 物品锁。
     */
    public Result<JSONObject> offer(OpenAppCredential credential, JSONObject body, String idempotencyKey) {
        String itemId = body == null ? null : body.getString("itemId");
        String toUserId = body == null ? null : body.getString("toUserId");
        if (StrUtil.isEmpty(itemId) || StrUtil.isEmpty(toUserId)) {
            return Result.error(400, "itemId 与 toUserId 不能为空");
        }
        if (toUserId.equals(credential.getBoundUid())) {
            return Result.error(400, "不能向自己发起流转");
        }
        String idemKey = idemKey(credential.getAppKey(), idempotencyKey);
        Result<JSONObject> replay = replay(idemKey);
        if (replay != null) {
            return replay;
        }
        // 接收方必须是平台 App 用户（决策②：按账号ID定位）
        AppUser receiver = appUserService.getById(toUserId);
        if (receiver == null) {
            return Result.error(404, "接收方用户不存在");
        }
        // 发起方持有校验（库存为准，与赠送三步流一致）
        ItemUserInventory inventory = itemUserInventoryService.queryUserItem(credential.getBoundUid(), itemId);
        if (inventory == null || inventory.getQuantity() == null || inventory.getQuantity() <= 0) {
            return Result.error(404, "物品不存在或不在凭据数据范围内");
        }
        ItemInfo item = itemInfoService.getById(itemId);
        if (item == null) {
            return Result.error(404, "物品不存在或不在凭据数据范围内");
        }
        // 实物在途校验（与赠送同一红线）
        Long activeCount = itemStorageInMapper.selectCount(new LambdaQueryWrapper<ItemStorageIn>()
                .eq(ItemStorageIn::getItemId, itemId)
                .in(ItemStorageIn::getStatus, StorageInConstant.ACTIVE_STATUSES));
        if (activeCount != null && activeCount > 0) {
            return Result.error(409, "物品存在在途仓单，暂不能发起流转");
        }
        // 流转锁：同一物品同时只允许一个待响应要约
        if (locked(itemId)) {
            return Result.error(409, "物品正在流转中（存在待响应要约），可先撤回或等待 72h 自动失效");
        }
        // MIP sensitivity 预检：restricted 档案禁止出域/流转
        String blocked = restrictedReason(item);
        if (blocked != null) {
            return Result.error(403, blocked);
        }
        int quantity = inventory.getQuantity();
        String token = IdUtil.fastSimpleUUID();
        JSONObject payload = new JSONObject(true);
        payload.put("appKey", credential.getAppKey());
        payload.put("itemId", String.valueOf(item.getId()));
        payload.put("oriId", item.getOriId());
        payload.put("fromUid", credential.getBoundUid());
        payload.put("toUserId", toUserId);
        payload.put("quantity", quantity);
        payload.put("itemName", item.getName());
        payload.put("itemIcon", item.getIcon());
        payload.put("itemCategory", item.getCategory());
        payload.put("offerAt", System.currentTimeMillis());
        redisUtil.set(KEY_FLOW_TOKEN + token, payload.toJSONString(), FLOW_WINDOW_SECONDS);
        redisUtil.set(KEY_FLOW_LOCK + itemId, token, FLOW_WINDOW_SECONDS);
        JSONObject out = new JSONObject(true);
        out.put("flowToken", token);
        out.put("expiresIn", FLOW_WINDOW_SECONDS);
        out.put("itemId", String.valueOf(item.getId()));
        out.put("oriId", item.getOriId());
        out.put("quantity", quantity);
        out.put("fromUserId", credential.getBoundUid());
        out.put("receiver", new JSONObject(true)
                .fluentPut("userId", toUserId)
                .fluentPut("nickname", StrUtil.blankToDefault(receiver.getNickname(), receiver.getUsername())));
        out.put("item", new JSONObject(true)
                .fluentPut("name", item.getName())
                .fluentPut("icon", item.getIcon())
                .fluentPut("category", item.getCategory()));
        out.put("warning", "要约 72 小时未响应将自动失效并解锁；锁定期内该物品不可赠送/编辑/删除/重复发起流转。仅接收方本人（或其本人凭据）可 accept/reject。");
        Result<JSONObject> result = Result.OK(out);
        result.setMessage("offered");
        cacheIdem(idemKey, result);
        return result;
    }

    /**
     * 接收方确认：仅接收方本人（凭据绑定用户 = 要约接收方）可确认；
     * 复核发起方仍持有与 sensitivity 后，同事务完成归属转移 + 账本 flow_out/flow_in 事件对，
     * 再作废 token 与解锁。全量转移（不支持数量拆分，拆分属 spawn 场景）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<JSONObject> accept(OpenAppCredential credential, String flowToken, String idempotencyKey) {
        if (StrUtil.isBlank(flowToken)) {
            return Result.error(400, "flowToken 不能为空");
        }
        String idemKey = idemKey(credential.getAppKey(), idempotencyKey);
        Result<JSONObject> replay = replay(idemKey);
        if (replay != null) {
            return replay;
        }
        String payloadJson = (String) redisUtil.get(KEY_FLOW_TOKEN + flowToken);
        if (payloadJson == null) {
            return Result.error(404, "flowToken 无效或已过期");
        }
        JSONObject payload = JSON.parseObject(payloadJson);
        // 决策②：仅接收方本人可确认，第三方不支持代收
        if (!credential.getBoundUid().equals(payload.getString("toUserId"))) {
            return Result.error(403, "仅接收方本人可确认流转（不支持代收）");
        }
        String itemId = payload.getString("itemId");
        // 锁一致性：锁内 token 必须与本 token 一致，否则视为已失效
        Object lock = redisUtil.get(KEY_FLOW_LOCK + itemId);
        if (lock == null || !flowToken.equals(String.valueOf(lock))) {
            redisUtil.del(KEY_FLOW_TOKEN + flowToken);
            return Result.error(409, "流转要约已失效");
        }
        String fromUid = payload.getString("fromUid");
        ItemUserInventory inventory = itemUserInventoryService.queryUserItem(fromUid, itemId);
        ItemInfo item = itemInfoService.getById(itemId);
        // 复核发起方仍持有（锁仅拦截开放接口，App 侧变动需现场复核）
        if (item == null || inventory == null || inventory.getQuantity() == null || inventory.getQuantity() <= 0) {
            releaseOffer(flowToken, itemId);
            return Result.error(409, "发起方已不持有该物品，要约失效");
        }
        // 数量红线：持有量被外部变动低于要约数量即失效（防按要约快照超发）
        int quantity = payload.getIntValue("quantity");
        if (inventory.getQuantity() < quantity) {
            releaseOffer(flowToken, itemId);
            return Result.error(409, "发起方持有数量已低于要约数量，要约失效");
        }
        // sensitivity 复检（发起后被编辑提级 restricted 的极端场景）
        String blocked = restrictedReason(item);
        if (blocked != null) {
            releaseOffer(flowToken, itemId);
            return Result.error(403, blocked);
        }
        String toUserId = credential.getBoundUid();
        // 1) 档案归属转移（条件更新：user_id 仍等于发起方才生效，并发 accept 时行锁串行化，仅首个成功）
        boolean claimed = itemInfoService.update(new LambdaUpdateWrapper<ItemInfo>()
                .set(ItemInfo::getUserId, toUserId)
                .set(ItemInfo::getUpdateTime, new Date())
                .eq(ItemInfo::getId, item.getId())
                .eq(ItemInfo::getUserId, fromUid));
        if (!claimed) {
            releaseOffer(flowToken, itemId);
            return Result.error(409, "发起方已不持有该物品，要约失效");
        }
        // 2) 发起方库存移除（全量流转）
        itemUserInventoryService.removeById(inventory.getId());
        // 3) 接收方库存（已有则累加）
        ItemUserInventory receiverInv = itemUserInventoryService.queryUserItem(toUserId, itemId);
        if (receiverInv != null) {
            receiverInv.setQuantity((receiverInv.getQuantity() == null ? 0 : receiverInv.getQuantity()) + quantity);
            itemUserInventoryService.updateItemById(receiverInv);
        } else {
            receiverInv = new ItemUserInventory();
            receiverInv.setItemId(itemId);
            receiverInv.setUserId(toUserId);
            receiverInv.setQuantity(quantity);
            receiverInv.setStatus(0);
            receiverInv.setIsDrop(0);
            receiverInv.setIsSync(0);
            receiverInv.setCreateTime(new Date());
            receiverInv.setGetTime(new Date());
            itemUserInventoryService.save(receiverInv);
        }
        // 4) 同步表（镜像赠送三步流语义：发起方清零置毁，接收方建副本/复位累加）
        // senderSync 必须限定发起方本人（同 itemId 多用户行时 getItemSyncByItemId 会取错行）
        ItemSync senderSync = itemSyncService.getItemSyncByOriIdAndUserId(item.getOriId(), fromUid);
        if (senderSync != null) {
            int remain = (senderSync.getQuantity() == null ? 0 : senderSync.getQuantity()) - quantity;
            senderSync.setQuantity(Math.max(remain, 0));
            if (remain <= 0) {
                senderSync.setStatus(SyncStatusEnum.DESTROYED.getCode());
            }
            senderSync.setUpdateTime(new Date());
            itemSyncService.updateById(senderSync);
        }
        // receiverSync 在 senderSync 更新之后读取：同一行（重新持有）场景读到清零快照，累加即得正确数量
        ItemSync receiverSync = itemSyncService.getItemSyncByOriIdAndUserId(item.getOriId(), toUserId);
        if (receiverSync == null) {
            ItemSync copy = new ItemSync();
            BeanUtil.copyProperties(senderSync, copy);
            copy.setId(null);
            copy.setOriId(item.getOriId());
            copy.setItemId(itemId);
            copy.setUserId(toUserId);
            copy.setQuantity(quantity);
            copy.setSyncStatus(SyncStatusEnum.UNSYNCED.getCode());
            // 接收方副本重置为有效态（源行可能是发起方的销毁行）
            copy.setStatus(SyncStatusEnum.SUCCESS.getCode());
            copy.setInventoryId(receiverInv.getId());
            copy.setVersion(1);
            copy.setCreateTime(new Date());
            copy.setUpdateTime(new Date());
            itemSyncService.save(copy);
        } else {
            receiverSync.setQuantity((receiverSync.getQuantity() == null ? 0 : receiverSync.getQuantity()) + quantity);
            // 复位：重新持有场景下行可能已被置毁（发起方清零段先执行），必须恢复有效态
            if (SyncStatusEnum.DESTROYED.getCode() == receiverSync.getStatus()) {
                receiverSync.setStatus(SyncStatusEnum.SUCCESS.getCode());
            }
            receiverSync.setSyncStatus(SyncStatusEnum.UNSYNCED.getCode());
            if (receiverSync.getVersion() == null) {
                receiverSync.setVersion(1);
            }
            receiverSync.setInventoryId(receiverInv.getId());
            receiverSync.setUpdateTime(new Date());
            itemSyncService.updateById(receiverSync);
        }
        // 5) 账本事件对（flow_out 发起方放行 → flow_in 接收方领取，同一哈希链）
        lineageService.append(new ItemLineage()
                .setOriId(item.getOriId())
                .setItemId(itemId)
                .setEventType(MipEventType.FLOW_OUT.getCode())
                .setFromUid(LineageService.hashUid(fromUid))
                .setToUid(LineageService.hashUid(toUserId))
                .setOperator(LineageService.operatorAgent(payload.getString("appKey")))
                .setQty(quantity)
                .setIdempotencyKey("flow_out:" + flowToken));
        lineageService.append(new ItemLineage()
                .setOriId(item.getOriId())
                .setItemId(itemId)
                .setEventType(MipEventType.FLOW_IN.getCode())
                .setFromUid(LineageService.hashUid(fromUid))
                .setToUid(LineageService.hashUid(toUserId))
                .setOperator(LineageService.operatorAgent(credential.getAppKey()))
                .setQty(quantity)
                .setIdempotencyKey("flow_in:" + flowToken));
        // 6) token 作废 + 解锁
        releaseOffer(flowToken, itemId);
        // 检索索引重建（归属转移后 uid 变更，同事务）
        searchIndexService.rebuild(itemId);
        JSONObject out = new JSONObject(true);
        out.put("flowToken", flowToken);
        out.put("itemId", itemId);
        out.put("oriId", item.getOriId());
        out.put("fromUserId", fromUid);
        out.put("toUserId", toUserId);
        out.put("quantity", quantity);
        out.put("acceptedAt", System.currentTimeMillis());
        out.put("item", new JSONObject(true)
                .fluentPut("name", item.getName())
                .fluentPut("icon", item.getIcon())
                .fluentPut("category", item.getCategory()));
        out.put("lineageDigest", lineageService.digest(item.getOriId()));
        Result<JSONObject> result = Result.OK(out);
        result.setMessage("accepted");
        cacheIdem(idemKey, result);
        return result;
    }

    /**
     * 接收方拒绝：仅接收方本人可拒绝；清 token 与解锁，不记账（custody 未变）。
     */
    public Result<JSONObject> reject(OpenAppCredential credential, String flowToken, String idempotencyKey) {
        return respond(credential, flowToken, "toUserId", "rejected", "仅接收方本人可拒绝流转", idempotencyKey);
    }

    /**
     * 发起方撤回：仅发起方可撤回；清 token 与解锁，不记账。
     */
    public Result<JSONObject> cancel(OpenAppCredential credential, String flowToken, String idempotencyKey) {
        return respond(credential, flowToken, "fromUid", "cancelled", "仅发起方可撤回流转要约", idempotencyKey);
    }

    // ================================ 读内核 ================================

    /**
     * 待处理要约列表（发出/收到，仅 Redis 存活的 pending 要约；
     * 已完成的要约以账本 flow_out/flow_in 事件为准）。
     *
     * @param dir sent / received / 空=all
     */
    public JSONObject list(String boundUid, String dir) {
        List<String> keys = redisUtil.scan(KEY_FLOW_TOKEN + "*");
        JSONArray sent = new JSONArray();
        JSONArray received = new JSONArray();
        if (keys != null) {
            for (String key : keys) {
                // scan 模式 open:flow:* 同时命中物品锁键，跳过
                if (StrUtil.isBlank(key) || key.startsWith(KEY_FLOW_LOCK)) {
                    continue;
                }
                String payloadJson = (String) redisUtil.get(key);
                if (StrUtil.isBlank(payloadJson)) {
                    continue;
                }
                JSONObject p;
                try {
                    p = JSON.parseObject(payloadJson);
                } catch (Exception e) {
                    continue;
                }
                long offerAt = p.getLongValue("offerAt");
                JSONObject entry = new JSONObject(true);
                entry.put("flowToken", key.substring(KEY_FLOW_TOKEN.length()));
                entry.put("itemId", p.getString("itemId"));
                entry.put("oriId", p.getString("oriId"));
                entry.put("itemName", p.getString("itemName"));
                entry.put("itemIcon", p.getString("itemIcon"));
                entry.put("itemCategory", p.getString("itemCategory"));
                entry.put("quantity", p.getIntValue("quantity"));
                entry.put("offerAt", offerAt);
                entry.put("expiresAt", offerAt + FLOW_WINDOW_SECONDS * 1000);
                boolean isSender = boundUid != null && boundUid.equals(p.getString("fromUid"));
                boolean isReceiver = boundUid != null && boundUid.equals(p.getString("toUserId"));
                if (isSender) {
                    entry.put("counterpartyUserId", p.getString("toUserId"));
                    sent.add(entry);
                }
                if (isReceiver) {
                    entry.put("counterpartyUserId", p.getString("fromUid"));
                    received.add(entry);
                }
            }
        }
        sent.sort((a, b) -> Long.compare(((JSONObject) b).getLongValue("offerAt"), ((JSONObject) a).getLongValue("offerAt")));
        received.sort((a, b) -> Long.compare(((JSONObject) b).getLongValue("offerAt"), ((JSONObject) a).getLongValue("offerAt")));
        JSONObject out = new JSONObject(true);
        boolean all = StrUtil.isBlank(dir) || "all".equals(dir);
        if (all || "sent".equals(dir)) {
            out.put("sent", sent);
        }
        if (all || "received".equals(dir)) {
            out.put("received", received);
        }
        return out;
    }

    // ================================ 出口复核 ================================

    /**
     * 物品是否处于流转锁定期（供 gift/update/delete/二次 offer 出口复核）
     */
    public boolean locked(String itemId) {
        if (StrUtil.isEmpty(itemId)) {
            return false;
        }
        return redisUtil.get(KEY_FLOW_LOCK + itemId) != null;
    }

    // ================================ 私有工具 ================================

    /**
     * reject/cancel 共用：校验操作方归属 → 锁一致性 → 清 token 与解锁
     */
    private Result<JSONObject> respond(OpenAppCredential credential, String flowToken,
                                       String partyField, String action, String denyMsg, String idempotencyKey) {
        if (StrUtil.isBlank(flowToken)) {
            return Result.error(400, "flowToken 不能为空");
        }
        String idemKey = idemKey(credential.getAppKey(), idempotencyKey);
        Result<JSONObject> replay = replay(idemKey);
        if (replay != null) {
            return replay;
        }
        String payloadJson = (String) redisUtil.get(KEY_FLOW_TOKEN + flowToken);
        if (payloadJson == null) {
            return Result.error(404, "flowToken 无效或已过期");
        }
        JSONObject payload = JSON.parseObject(payloadJson);
        if (!credential.getBoundUid().equals(payload.getString(partyField))) {
            return Result.error(403, denyMsg);
        }
        String itemId = payload.getString("itemId");
        Object lock = redisUtil.get(KEY_FLOW_LOCK + itemId);
        if (lock != null && !flowToken.equals(String.valueOf(lock))) {
            // 锁已被新要约占用：本 token 为残余，仅清理自身
            redisUtil.del(KEY_FLOW_TOKEN + flowToken);
            return Result.error(409, "流转要约已失效");
        }
        releaseOffer(flowToken, itemId);
        JSONObject out = new JSONObject(true);
        out.put("flowToken", flowToken);
        out.put("itemId", itemId);
        out.put("oriId", payload.getString("oriId"));
        out.put("itemName", payload.getString("itemName"));
        out.put("action", action);
        Result<JSONObject> result = Result.OK(out);
        result.setMessage(action);
        cacheIdem(idemKey, result);
        return result;
    }

    /**
     * MIP sensitivity 预检：任一组件 restricted 即禁止流转（出域红线，档案级拦截）
     */
    private String restrictedReason(ItemInfo item) {
        if (item == null || StrUtil.isBlank(item.getFields())) {
            return null;
        }
        try {
            JSONObject profile = MipProfileBuilder.build(item.getFields(), null, null);
            JSONArray properties = profile.getJSONArray("properties");
            if (properties == null) {
                return null;
            }
            for (int i = 0; i < properties.size(); i++) {
                JSONObject p = properties.getJSONObject(i);
                if (MipComponentRegistry.SENS_RESTRICTED.equals(p.getString("sensitivity"))) {
                    return "档案含受限字段（element " + p.getIntValue("element") + " restricted），禁止流转";
                }
            }
        } catch (Exception e) {
            log.warn("[open-flow] sensitivity 预检失败 itemId={}: {}", item.getId(), e.getMessage());
            return "档案解析失败，禁止流转";
        }
        return null;
    }

    /**
     * 清理要约：token 作废 + 解锁
     */
    private void releaseOffer(String flowToken, String itemId) {
        redisUtil.del(KEY_FLOW_TOKEN + flowToken);
        redisUtil.del(KEY_FLOW_LOCK + itemId);
    }

    private String idemKey(String appKey, String idempotencyKey) {
        return "open:idem:" + appKey + ":" + idempotencyKey;
    }

    /**
     * 幂等回放（手动重建 Result，避免泛型反序列化差异，与 AppOpenService 同规则）
     */
    private Result<JSONObject> replay(String idemKey) {
        Object cached = redisUtil.get(idemKey);
        if (cached == null) {
            return null;
        }
        JSONObject c = JSON.parseObject(String.valueOf(cached));
        Result<JSONObject> r = new Result<>();
        r.setCode(c.getIntValue("code"));
        r.setSuccess(c.getBooleanValue("success"));
        r.setMessage(c.getString("message"));
        r.setResult(c.getJSONObject("result"));
        return r;
    }

    private void cacheIdem(String idemKey, Result<JSONObject> result) {
        redisUtil.set(idemKey, JSON.toJSONString(result), IDEM_TTL_SECONDS);
    }
}
