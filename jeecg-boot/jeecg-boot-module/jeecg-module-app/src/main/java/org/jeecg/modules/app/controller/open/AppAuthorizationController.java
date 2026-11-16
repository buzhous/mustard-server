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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开放授权管理：用户在个人中心开启/关闭开放授权、重置授权密钥。
 * 凭据仅含 AppKey（随机 SHA-1 哈希，40 位十六进制，参数无需加密/签名），
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

    @Operation(summary = "授权信息（未开启时返回空）")
    @GetMapping("/info")
    public Result<OpenAppCredential> info() {
        return Result.OK(sanitize(openAppCredentialService.getByBoundUid(currentUid())));
    }

    @Operation(summary = "开启授权（无凭据则自动创建）")
    @PostMapping("/enable")
    public Result<OpenAppCredential> enable() {
        return Result.OK(sanitize(openAppCredentialService.enableByUser(currentUid())));
    }

    @Operation(summary = "关闭授权")
    @PostMapping("/disable")
    public Result<OpenAppCredential> disable() {
        OpenAppCredential credential = openAppCredentialService.disableByUser(currentUid());
        if (credential == null) {
            return Result.error("尚未开启授权");
        }
        return Result.OK(sanitize(credential));
    }

    @Operation(summary = "重置授权密钥（原密钥立即失效）")
    @PostMapping("/reset")
    public Result<OpenAppCredential> reset() {
        return Result.OK(sanitize(openAppCredentialService.resetSecretByUser(currentUid())));
    }

    /**
     * 对外不下发 appSecret（鉴权仅用 appKey，历史遗留字段置空返回）
     */
    private OpenAppCredential sanitize(OpenAppCredential credential) {
        if (credential != null) {
            credential.setAppSecret(null);
        }
        return credential;
    }
}
