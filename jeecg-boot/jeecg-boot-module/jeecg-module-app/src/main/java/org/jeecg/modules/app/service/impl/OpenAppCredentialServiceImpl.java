package org.jeecg.modules.app.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.app.entity.OpenAppCredential;
import org.jeecg.modules.app.mapper.OpenAppCredentialMapper;
import org.jeecg.modules.app.service.IOpenAppCredentialService;
import org.springframework.stereotype.Service;

@Service
public class OpenAppCredentialServiceImpl
        extends ServiceImpl<OpenAppCredentialMapper, OpenAppCredential>
        implements IOpenAppCredentialService {

    @Override
    public OpenAppCredential getEnabledByAppKey(String appKey) {
        if (StrUtil.isEmpty(appKey)) {
            return null;
        }
        return this.getOne(new LambdaQueryWrapper<OpenAppCredential>()
                .eq(OpenAppCredential::getAppKey, appKey.trim())
                .eq(OpenAppCredential::getStatus, 1)
                .last("limit 1"), false);
    }
}
