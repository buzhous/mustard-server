package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.entity.OpenAppCredential;

import java.util.List;

public interface IOpenAppCredentialService extends IService<OpenAppCredential> {

    /**
     * 按 AppKey 查询启用的凭据（多密钥下任意一条启用即可通过）
     */
    OpenAppCredential getEnabledByAppKey(String appKey);

    /**
     * 按绑定用户查询凭据列表（创建时间倒序）
     */
    List<OpenAppCredential> listByUid(String uid);

    /**
     * 用户新增密钥：随机生成 AppKey 并启用（受每用户上限约束）
     */
    OpenAppCredential addByUser(String uid, String appName);

    /**
     * 用户重命名密钥
     */
    OpenAppCredential renameByUser(String uid, String credentialId, String appName);

    /**
     * 用户启用单条密钥
     */
    OpenAppCredential enableByUser(String uid, String credentialId);

    /**
     * 用户禁用单条密钥（保留密钥）
     */
    OpenAppCredential disableByUser(String uid, String credentialId);

    /**
     * 用户删除单条密钥
     */
    void deleteByUser(String uid, String credentialId);

    /**
     * 用户设置密钥 scope 权限分级（readonly/readwrite 单向授权可随时回收）
     *
     * @return 更新后的凭据（不含敏感字段）
     */
    OpenAppCredential updateScopeByUser(String uid, String credentialId, String scope);

    /**
     * 配置 Webhook 回调地址（secret 每次配置重新生成，仅配置响应返回一次）
     *
     * @return 更新后的凭据（含 webhookSecret，调用方须仅本次下发）
     */
    OpenAppCredential setWebhookByUser(String uid, String credentialId, String webhookUrl);

    /**
     * 清除 Webhook 订阅（停止事件推送）
     */
    OpenAppCredential clearWebhookByUser(String uid, String credentialId);

    /**
     * 校验密钥归属（不存在或不属于当前用户抛参数错误）
     */
    OpenAppCredential getOwnedByUser(String uid, String credentialId);
}
