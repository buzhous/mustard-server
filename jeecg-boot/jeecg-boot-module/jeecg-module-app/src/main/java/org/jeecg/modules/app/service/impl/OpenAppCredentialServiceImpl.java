package org.jeecg.modules.app.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.entity.OpenAppCredential;
import org.jeecg.modules.app.mapper.OpenAppCredentialMapper;
import org.jeecg.modules.app.service.IOpenAppCredentialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class OpenAppCredentialServiceImpl
        extends ServiceImpl<OpenAppCredentialMapper, OpenAppCredential>
        implements IOpenAppCredentialService {

    /** 每用户密钥数量上限 */
    private static final int MAX_CREDENTIALS_PER_USER = 10;

    /** 密钥名称最大长度 */
    private static final int MAX_APP_NAME_LENGTH = 20;

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
    public List<OpenAppCredential> listByUid(String uid) {
        if (StrUtil.isEmpty(uid)) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        return this.list(new LambdaQueryWrapper<OpenAppCredential>()
                .eq(OpenAppCredential::getBoundUid, uid)
                .orderByDesc(OpenAppCredential::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpenAppCredential addByUser(String uid, String appName) {
        if (StrUtil.isEmpty(uid)) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        long count = this.count(new LambdaQueryWrapper<OpenAppCredential>()
                .eq(OpenAppCredential::getBoundUid, uid));
        if (count >= MAX_CREDENTIALS_PER_USER) {
            throw new AppException(ExceptionEnum.OPEN_CREDENTIAL_LIMIT);
        }
        String name = normalizeAppName(appName);
        if (StrUtil.isEmpty(name)) {
            name = "密钥" + (count + 1);
        }
        Date now = new Date();
        OpenAppCredential credential = new OpenAppCredential()
                .setAppKey(generateUniqueAppKey())
                .setAppName(name)
                .setBoundUid(uid)
                .setStatus(1)
                .setCreateTime(now)
                .setUpdateTime(now);
        this.save(credential);
        return credential;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpenAppCredential renameByUser(String uid, String credentialId, String appName) {
        OpenAppCredential credential = getOwned(uid, credentialId);
        String name = normalizeAppName(appName);
        if (StrUtil.isEmpty(name)) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        credential.setAppName(name);
        credential.setUpdateTime(new Date());
        this.updateById(credential);
        return credential;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpenAppCredential enableByUser(String uid, String credentialId) {
        OpenAppCredential credential = getOwned(uid, credentialId);
        credential.setStatus(1);
        credential.setUpdateTime(new Date());
        this.updateById(credential);
        return credential;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpenAppCredential disableByUser(String uid, String credentialId) {
        OpenAppCredential credential = getOwned(uid, credentialId);
        credential.setStatus(0);
        credential.setUpdateTime(new Date());
        this.updateById(credential);
        return credential;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByUser(String uid, String credentialId) {
        OpenAppCredential credential = getOwned(uid, credentialId);
        this.removeById(credential.getId());
    }

    /**
     * 校验密钥归属：不存在或不属于当前用户均按参数错误处理
     */
    private OpenAppCredential getOwned(String uid, String credentialId) {
        if (StrUtil.hasEmpty(uid, credentialId)) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        OpenAppCredential credential = this.getById(credentialId.trim());
        if (credential == null || !Objects.equals(credential.getBoundUid(), uid)) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        return credential;
    }

    private String normalizeAppName(String appName) {
        if (StrUtil.isEmpty(appName)) {
            return "";
        }
        return appName.trim().length() > MAX_APP_NAME_LENGTH
                ? appName.trim().substring(0, MAX_APP_NAME_LENGTH)
                : appName.trim();
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
