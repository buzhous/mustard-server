package org.jeecg.modules.app.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.app.entity.OpenAppCredential;
import org.jeecg.modules.app.mapper.OpenAppCredentialMapper;
import org.jeecg.modules.app.service.IOpenAppCredentialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

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

    @Override
    public OpenAppCredential getByBoundUid(String uid) {
        if (StrUtil.isEmpty(uid)) {
            return null;
        }
        return this.getOne(new LambdaQueryWrapper<OpenAppCredential>()
                .eq(OpenAppCredential::getBoundUid, uid)
                .orderByDesc(OpenAppCredential::getCreateTime)
                .last("limit 1"), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpenAppCredential enableByUser(String uid) {
        OpenAppCredential credential = getByBoundUid(uid);
        if (credential == null) {
            credential = new OpenAppCredential()
                    .setAppKey(generateUniqueAppKey())
                    .setAppSecret(RandomUtil.randomString(32))
                    .setAppName("个人开放授权")
                    .setBoundUid(uid)
                    .setStatus(1)
                    .setCreateTime(new Date())
                    .setUpdateTime(new Date());
            this.save(credential);
            return credential;
        }
        credential.setStatus(1);
        credential.setUpdateTime(new Date());
        this.updateById(credential);
        return credential;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpenAppCredential disableByUser(String uid) {
        OpenAppCredential credential = getByBoundUid(uid);
        if (credential == null) {
            return null;
        }
        credential.setStatus(0);
        credential.setUpdateTime(new Date());
        this.updateById(credential);
        return credential;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpenAppCredential resetSecretByUser(String uid) {
        OpenAppCredential credential = getByBoundUid(uid);
        if (credential == null) {
            // 未开启过授权时，重置等价于首次开启
            return enableByUser(uid);
        }
        credential.setAppKey(generateUniqueAppKey());
        credential.setAppSecret(RandomUtil.randomString(32));
        credential.setStatus(1);
        credential.setUpdateTime(new Date());
        this.updateById(credential);
        return credential;
    }

    /**
     * 生成唯一 AppKey，冲突时重试
     */
    private String generateUniqueAppKey() {
        for (int i = 0; i < 5; i++) {
            String appKey = RandomUtil.randomStringUpper(16);
            long count = this.count(new LambdaQueryWrapper<OpenAppCredential>()
                    .eq(OpenAppCredential::getAppKey, appKey));
            if (count == 0) {
                return appKey;
            }
        }
        // 兜底：时间戳拼接保证唯一
        return RandomUtil.randomStringUpper(8) + System.currentTimeMillis();
    }
}
