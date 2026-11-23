package org.jeecg.modules.app.service.impl;

import org.jeecg.modules.app.service.StatsEventService;
import org.springframework.beans.factory.annotation.Autowired;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.entity.OpenAppCredential;
import org.jeecg.modules.app.mapper.OpenAppCredentialMapper;
import org.jeecg.modules.app.openapi.OpenScopeChecker;
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

    @Autowired
    private StatsEventService statsEventService;

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
        // F1 运营统计：AppKey 激活成功（status 0→1）
        statsEventService.fire("appkey_activated", uid, null);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpenAppCredential updateScopeByUser(String uid, String credentialId, String scope) {
        if (StrUtil.isEmpty(scope)
                || !OpenScopeChecker.SCOPE_READONLY.equals(scope)
                && !OpenScopeChecker.SCOPE_READWRITE.equals(scope)) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        OpenAppCredential credential = getOwned(uid, credentialId);
        credential.setScope(scope);
        credential.setScopeUpdatedAt(new Date());
        credential.setUpdateTime(new Date());
        this.updateById(credential);
        credential.setAppSecret(null);
        return credential;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpenAppCredential setWebhookByUser(String uid, String credentialId, String webhookUrl) {
        String url = webhookUrl == null ? "" : webhookUrl.trim();
        if (StrUtil.hasEmpty(uid, credentialId) || url.length() > 255
                || !(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        OpenAppCredential credential = getOwned(uid, credentialId);
        credential.setWebhookUrl(url);
        // secret 每次配置重新生成（支持轮换），仅配置响应返回一次
        credential.setWebhookSecret(IdUtil.simpleUUID());
        credential.setUpdateTime(new Date());
        this.updateById(credential);
        return credential;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpenAppCredential clearWebhookByUser(String uid, String credentialId) {
        if (StrUtil.hasEmpty(uid, credentialId)) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        OpenAppCredential credential = getOwned(uid, credentialId);
        // updateById 忽略 null 字段，清空需 UpdateWrapper 显式置 null
        this.update(new LambdaUpdateWrapper<OpenAppCredential>()
                .eq(OpenAppCredential::getId, credential.getId())
                .set(OpenAppCredential::getWebhookUrl, null)
                .set(OpenAppCredential::getWebhookSecret, null)
                .set(OpenAppCredential::getUpdateTime, new Date()));
        // 同步内存对象，避免返回更新前的旧值
        credential.setWebhookUrl(null);
        credential.setWebhookSecret(null);
        return credential;
    }

    /**
     * 校验密钥归属：不存在或不属于当前用户均按参数错误处理
     */
    @Override
    public OpenAppCredential getOwnedByUser(String uid, String credentialId) {
        return getOwned(uid, credentialId);
    }

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
