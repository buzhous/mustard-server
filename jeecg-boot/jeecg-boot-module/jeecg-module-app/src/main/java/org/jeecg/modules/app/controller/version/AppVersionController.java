package org.jeecg.modules.app.controller.version;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.entity.AppVersion;
import org.jeecg.modules.app.mapper.AppVersionMapper;
import org.jeecg.modules.app.service.AppVersionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * App 版本管理（版本管理策略 §04）：
 * - GET /app/version/latest：App 更新检查（匿名可调；AppAuthFilter 放行，见 AppAuthFilter）；
 * - /app/version/admin/**：后台管理（list / save / delete / rollback），鉴权约定与 /app/stats/admin 一致（部署侧内网隔离 + 角色授权）。
 */
@Slf4j
@Tag(name = "App 版本管理")
@RestController
@RequestMapping("/app/version")
@RequiredArgsConstructor
public class AppVersionController {

    private final AppVersionService versionService;
    private final AppVersionMapper versionMapper;

    // ==================== App 更新检查 ====================

    @Operation(summary = "最新版本判定（匿名可调）")
    @GetMapping("/latest")
    public Result<JSONObject> latest(@RequestParam(defaultValue = "") String platform,
                                     @RequestParam(defaultValue = "0") int buildNo,
                                     @RequestParam(defaultValue = "") String deviceId) {
        if (!"ios".equals(platform) && !"android".equals(platform)) {
            return Result.error("platform 仅支持 ios / android");
        }
        if (buildNo < 0) {
            return Result.error("buildNo 非法");
        }
        return Result.OK(versionService.latest(platform, buildNo, deviceId));
    }

    // ==================== 后台管理 ====================

    @Operation(summary = "版本记录分页（platform/status 筛选）")
    @GetMapping("/admin/list")
    public Result<Page<AppVersion>> list(@RequestParam(defaultValue = "1") Integer pageNo,
                                         @RequestParam(defaultValue = "20") Integer pageSize,
                                         @RequestParam(required = false) String platform,
                                         @RequestParam(required = false) Integer status) {
        pageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<AppVersion> qw = new LambdaQueryWrapper<AppVersion>()
                .orderByDesc(AppVersion::getPublishTime)
                .orderByDesc(AppVersion::getBuildNo);
        if (platform != null && !platform.isBlank()) {
            qw.eq(AppVersion::getPlatform, platform);
        }
        if (status != null) {
            qw.eq(AppVersion::getStatus, status);
        }
        return Result.OK(versionMapper.selectPage(new Page<>(pageNo, pageSize), qw));
    }

    @Operation(summary = "新建/编辑版本记录")
    @PostMapping("/admin/save")
    public Result<AppVersion> save(@RequestBody AppVersion body) {
        try {
            return Result.OK(versionService.saveVersion(body, "admin"));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "回滚：历史版本重新置为全量")
    @PostMapping("/admin/rollback")
    public Result<Void> rollback(@RequestBody JSONObject body) {
        String platform = body.getString("platform");
        Long targetId = body.getLong("targetId");
        if (!Set.of("ios", "android").contains(platform) || targetId == null) {
            return Result.error("platform/targetId 非法");
        }
        try {
            versionService.rollback(platform, targetId);
            return Result.OK();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "删除（仅草稿与已下线）")
    @PostMapping("/admin/delete")
    public Result<Void> delete(@RequestBody JSONObject body) {
        Long id = body.getLong("id");
        if (id == null) {
            return Result.error("id 不能为空");
        }
        try {
            versionService.delete(id);
            return Result.OK();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
