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
}
