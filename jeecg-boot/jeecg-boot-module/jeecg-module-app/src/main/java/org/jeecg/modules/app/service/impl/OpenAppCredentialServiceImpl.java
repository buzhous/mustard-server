package org.jeecg.modules.app.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.app.entity.OpenAppCredential;
import org.jeecg.modules.app.mapper.OpenAppCredentialMapper;
import org.jeecg.modules.app.service.IOpenAppCredentialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

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
        credential.setStatus(1);
        credential.setUpdateTime(new Date());
        this.updateById(credential);
        return credential;
    }

    /**
     * 生成唯一 AppKey：随机 SHA-1 哈希值（40 位小写十六进制），冲突时重试
     */
    private String generateUniqueAppKey() {
        for (int i = 0; i < 5; i++) {
            String appKey = SecureUtil.sha1(UUID.randomUUID() + ":" + System.nanoTime());
            long count = this.count(new LambdaQueryWrapper<OpenAppCredential>()
                    .eq(OpenAppCredential::getAppKey, appKey));
            if (count == 0) {
                return appKey;
            }
        }
        // 兜底：哈希拼接时间戳保证唯一
        return SecureUtil.sha1(UUID.randomUUID() + ":" + System.currentTimeMillis());
    }
}
