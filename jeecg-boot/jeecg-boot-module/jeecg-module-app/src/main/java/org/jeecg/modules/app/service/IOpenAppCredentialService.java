package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.entity.OpenAppCredential;

public interface IOpenAppCredentialService extends IService<OpenAppCredential> {

    /**
     * 按 AppKey 查询启用的凭据
     */
    OpenAppCredential getEnabledByAppKey(String appKey);

    /**
     * 按绑定用户查询凭据（不限制状态）
     */
    OpenAppCredential getByBoundUid(String uid);

    /**
     * 用户开启开放授权：无凭据则自动创建（AK/SK 随机生成，状态启用）
     */
    OpenAppCredential enableByUser(String uid);

    /**
     * 用户关闭开放授权（保留凭据与密钥）
     */
    OpenAppCredential disableByUser(String uid);

    /**
     * 用户重置授权密钥：重新生成 AppKey（随机 SHA-1 哈希）并启用
     */
    OpenAppCredential resetSecretByUser(String uid);
}
