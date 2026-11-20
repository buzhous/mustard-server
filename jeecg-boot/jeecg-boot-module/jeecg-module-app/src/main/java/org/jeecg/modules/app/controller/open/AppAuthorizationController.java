package org.jeecg.modules.app.controller.open;

import cn.hutool.core.util.ObjectUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.entity.OpenAppCredential;
import org.jeecg.modules.app.entity.user.AppUser;
import org.jeecg.modules.app.service.IOpenAppCredentialService;
import org.jeecg.modules.app.utils.AppAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        }
        return credentials;
    }

    private OpenAppCredential sanitize(OpenAppCredential credential) {
        if (credential != null) {
            credential.setAppSecret(null);
        }
        return credential;
    }
}
