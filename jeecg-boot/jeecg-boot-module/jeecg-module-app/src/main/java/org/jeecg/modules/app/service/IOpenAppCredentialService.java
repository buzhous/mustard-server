package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.entity.OpenAppCredential;

public interface IOpenAppCredentialService extends IService<OpenAppCredential> {

    /**
     * 按 AppKey 查询启用的凭据
     */
    OpenAppCredential getEnabledByAppKey(String appKey);
}
