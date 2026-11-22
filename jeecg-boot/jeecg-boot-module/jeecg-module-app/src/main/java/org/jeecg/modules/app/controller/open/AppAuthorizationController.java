package org.jeecg.modules.app.controller.open;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.entity.OpenAppCredential;
import org.jeecg.modules.app.entity.OpenAuditLog;
import org.jeecg.modules.app.entity.OpenWebhookEvent;
import org.jeecg.modules.app.entity.user.AppUser;
import org.jeecg.modules.app.mapper.OpenAuditLogMapper;
import org.jeecg.modules.app.mapper.OpenWebhookEventMapper;
import org.jeecg.modules.app.service.IOpenAppCredentialService;
import org.jeecg.modules.app.utils.AppAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 开放授权管理：用户在个人中心管理多个授权密钥（新增/命名/启用禁用/删除）。
 * 每条密钥为独立 AppKey（随机 SHA-1 哈希，40 位十六进制，参数无需加密/签名），
 * 绑定当前用户，供外部系统通过开放接口（/app/open/**）访问该用户的数据范围。
 */
@Slf4j
@Tag(name = "开放授权")
@RestController
@RequestMapping("/app/authorization")
public class AppAuthorizationController {

    @Autowired
    private IOpenAppCredentialService openAppCredentialService;

    @Autowired
    private OpenAuditLogMapper openAuditLogMapper;

    @Autowired
    private OpenWebhookEventMapper openWebhookEventMapper;

    private String currentUid() {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            throw new AppException(ExceptionEnum.USER_INFO_NOT_EXIST);
        }
        return loginUser.getId();
    }

    @Operation(summary = "密钥列表（未创建时返回空数组）")
    @GetMapping("/info")
    public Result<List<OpenAppCredential>> info() {
        return Result.OK(sanitizeList(openAppCredentialService.listByUid(currentUid())));
    }

    @Operation(summary = "新增密钥（可自定义名称，上限10个）")
    @PostMapping("/add")
    public Result<OpenAppCredential> add(@RequestBody(required = false) Map<String, Object> body) {
        String appName = body == null ? null : (body.get("appName") == null ? null : body.get("appName").toString());
        return Result.OK(sanitize(openAppCredentialService.addByUser(currentUid(), appName)));
    }

    @Operation(summary = "重命名密钥")
    @PostMapping("/rename")
    public Result<OpenAppCredential> rename(@RequestBody Map<String, Object> body) {
        return Result.OK(sanitize(openAppCredentialService.renameByUser(
                currentUid(), stringValue(body, "credentialId"), stringValue(body, "appName"))));
    }

    @Operation(summary = "启用单条密钥")
    @PostMapping("/enable")
    public Result<OpenAppCredential> enable(@RequestBody Map<String, Object> body) {
        return Result.OK(sanitize(openAppCredentialService.enableByUser(
                currentUid(), stringValue(body, "credentialId"))));
    }

    @Operation(summary = "禁用单条密钥（保留密钥）")
    @PostMapping("/disable")
    public Result<OpenAppCredential> disable(@RequestBody Map<String, Object> body) {
        return Result.OK(sanitize(openAppCredentialService.disableByUser(
                currentUid(), stringValue(body, "credentialId"))));
    }

    @Operation(summary = "删除单条密钥")
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Map<String, Object> body) {
        openAppCredentialService.deleteByUser(currentUid(), stringValue(body, "credentialId"));
        return Result.OK();
    }

    @Operation(summary = "设置密钥 scope 权限分级（readwrite 授予/回收需前端二次确认弹窗）")
    @PostMapping("/scope")
    public Result<OpenAppCredential> setScope(@RequestBody Map<String, Object> body) {
        return Result.OK(sanitize(openAppCredentialService.updateScopeByUser(
                currentUid(), stringValue(body, "credentialId"), stringValue(body, "scope"))));
    }

    @Operation(summary = "更新应用信息（名称/描述/权限，开放平台控制台使用；描述落 remark 字段）")
    @PostMapping("/update")
    public Result<OpenAppCredential> update(@RequestBody Map<String, Object> body) {
        String credentialId = stringValue(body, "credentialId");
        String appName = stringValue(body, "appName");
        String description = body.get("description") == null ? "" : body.get("description").toString();
        String scope = stringValue(body, "scope");
        // 名称：重名校验与归属校验在 renameByUser 内
        openAppCredentialService.renameByUser(currentUid(), credentialId, appName);
        // 描述：透传至 remark，允许空串清空
        OpenAppCredential patch = new OpenAppCredential();
        patch.setId(credentialId);
        patch.setRemark(description);
        openAppCredentialService.updateById(patch);
        // 权限：readonly / readwrite
        openAppCredentialService.updateScopeByUser(currentUid(), credentialId, scope);
        return Result.OK(sanitize(openAppCredentialService.getOwnedByUser(currentUid(), credentialId)));
    }

    @Operation(summary = "密钥调用记录（授权页透明视图：谁在何时读了/写了我的物品）")
    @GetMapping("/calls")
    public Result<Page<OpenAuditLog>> calls(
            @RequestParam(name = "credentialId") String credentialId,
            @RequestParam(name = "pageNo", required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        OpenAppCredential credential = openAppCredentialService.getOwnedByUser(currentUid(), credentialId);
        Page<OpenAuditLog> page = openAuditLogMapper.selectPage(
                new Page<>(pageNo, Math.min(pageSize, 50)),
                new LambdaQueryWrapper<OpenAuditLog>()
                        .eq(OpenAuditLog::getAppKey, credential.getAppKey())
                        .orderByDesc(OpenAuditLog::getId));
        return Result.OK(page);
    }

    @Operation(summary = "配置 Webhook 回调地址（事件推送 item.changed/item.flowed；secret 每次配置重新生成，仅本次返回）")
    @PostMapping("/webhook")
    public Result<Map<String, Object>> setWebhook(@RequestBody Map<String, Object> body) {
        OpenAppCredential credential = openAppCredentialService.setWebhookByUser(
                currentUid(), stringValue(body, "credentialId"), stringValue(body, "webhookUrl"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("credentialId", credential.getId());
        out.put("webhookUrl", credential.getWebhookUrl());
        out.put("webhookSecret", credential.getWebhookSecret());
        return Result.OK(out);
    }

    @Operation(summary = "清除 Webhook 订阅（停止事件推送）")
    @PostMapping("/webhook/clear")
    public Result<OpenAppCredential> clearWebhook(@RequestBody Map<String, Object> body) {
        return Result.OK(sanitize(openAppCredentialService.clearWebhookByUser(
                currentUid(), stringValue(body, "credentialId"))));
    }

    @Operation(summary = "Webhook 投递记录（事件/状态/重试次数/最近响应码，审计透明视图）")
    @GetMapping("/webhook/events")
    public Result<Page<OpenWebhookEvent>> webhookEvents(
            @RequestParam(name = "credentialId") String credentialId,
            @RequestParam(name = "pageNo", required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        OpenAppCredential credential = openAppCredentialService.getOwnedByUser(currentUid(), credentialId);
        Page<OpenWebhookEvent> page = openWebhookEventMapper.selectPage(
                new Page<>(pageNo, Math.min(pageSize, 50)),
                new LambdaQueryWrapper<OpenWebhookEvent>()
                        .eq(OpenWebhookEvent::getAppKey, credential.getAppKey())
                        .orderByDesc(OpenWebhookEvent::getId));
        return Result.OK(page);
    }

    private String stringValue(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        return body.get(key).toString();
    }

    /**
     * 对外不下发 appSecret（鉴权仅用 appKey，历史遗留字段置空返回）
     */
    private List<OpenAppCredential> sanitizeList(List<OpenAppCredential> credentials) {
        for (OpenAppCredential credential : credentials) {
            credential.setAppSecret(null);
            credential.setWebhookSecret(null);
        }
        return credentials;
    }

    private OpenAppCredential sanitize(OpenAppCredential credential) {
        if (credential != null) {
            credential.setAppSecret(null);
            credential.setWebhookSecret(null);
        }
        return credential;
    }
}
