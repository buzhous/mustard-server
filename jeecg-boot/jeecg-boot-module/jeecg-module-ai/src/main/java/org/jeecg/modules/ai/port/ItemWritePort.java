package org.jeecg.modules.ai.port;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.api.vo.Result;

/**
 * 物品写端口：ai 模块创建物品（app 模块桥接实现）
 * <p>
 * 实现类内部完成 AI 凭据解析（readwrite + ai_assistant 标记优先，无则自动创建，
 * 即原 AiAgentService.resolveCredential 全部逻辑）并生成幂等键 IdUtil.fastSimpleUUID()，
 * 最终调用 AppOpenService.addItem(credential, body, idemKey)。
 */
public interface ItemWritePort {

    /**
     * 创建物品（每次调用独立解析凭据并生成独立幂等键，N 次调用 = N 个幂等键，与原循环行为一致）
     *
     * @param uid  用户 uid
     * @param body 开放接口 add_item 请求体（attributes/confidence/source 等）
     * @return 开放接口创建结果（成功时 result 为 MIP 出域实例）
     */
    Result<JSONObject> addItem(String uid, JSONObject body);

    /**
     * 编辑物品（P5 update 意图）：局部补丁语义（body.patch=true 强制置入），
     * 服务端读存量 fields 按 element 替换/追加合并（存量 restricted 原值保留，
     * patch 中 restricted 元素丢弃），后走与新增相同的 MIP 写入链 + 账本 update 事件。
     * 每次调用独立解析凭据并生成独立幂等键；成功后失效该 uid 查询意图缓存。
     *
     * @param uid    用户 uid
     * @param itemId 目标物品 id（tbl_item_info.id）
     * @param body   局部补丁请求体（attributes=[{element,value}…]/source；不传 confidence 保持原状态）
     * @return 开放接口更新结果（成功时 result 为 MIP 出域实例）
     */
    Result<JSONObject> updateItem(String uid, String itemId, JSONObject body);
}
