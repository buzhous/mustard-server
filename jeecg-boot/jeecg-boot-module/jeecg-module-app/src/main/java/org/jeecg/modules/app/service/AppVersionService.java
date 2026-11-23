package org.jeecg.modules.app.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.app.entity.AppVersion;
import org.jeecg.modules.app.mapper.AppVersionMapper;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * App 版本管理（版本管理策略 §04）：latest 判定 / 灰度分桶 / 强更门禁。
 *
 * 判定口径（实施决策，与策略 §4.2 的差异修正点）：
 * - 强更不受灰度限制：forceUpgrade=1 且 clientBuild &lt; minBuild 时无论是否命中灰度都返回 force=true——
 *   门禁是安全底线，灰度只控制「可选更新提醒」的放量范围；
 * - hasUpdate = force ||（latest.buildNo &gt; clientBuild 且灰度命中）；
 * - deviceId 稳定分桶：String.hashCode 取模，同一设备判定结果恒定（不会反复弹/不弹）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppVersionService {

    private final AppVersionMapper versionMapper;

    /** 该平台最新发布中的记录（status 1 灰度中 / 2 全量，按发布时间取最新；同秒并列时以 buildNo 大者兜底——MySQL DATETIME 秒级精度） */
    public AppVersion findLatest(String platform) {
        return versionMapper.selectOne(new LambdaQueryWrapper<AppVersion>()
                .eq(AppVersion::getPlatform, platform)
                .in(AppVersion::getStatus, 1, 2)
                .orderByDesc(AppVersion::getPublishTime)
                .orderByDesc(AppVersion::getBuildNo)
                .last("limit 1"));
    }

    /**
     * latest 判定（匿名可调）。
     *
     * @param platform    ios / android
     * @param clientBuild 客户端构建号（pubspec build，AppMeta.buildNumber）
     * @param deviceId    设备假名（灰度分桶；空视为未命中灰度，仅强更生效）
     */
    public JSONObject latest(String platform, int clientBuild, String deviceId) {
        JSONObject data = new JSONObject();
        data.put("hasUpdate", false);
        data.put("force", false);

        AppVersion v = findLatest(platform);
        if (v == null || v.getBuildNo() == null) {
            return data;
        }

        boolean force = v.getForceUpgrade() != null && v.getForceUpgrade() == 1
                && v.getMinBuild() != null
                && clientBuild < v.getMinBuild();
        boolean inRollout = deviceId != null && !deviceId.isEmpty()
                && v.getRolloutPct() != null
                && Math.floorMod(Objects.hashCode(deviceId), 100) < v.getRolloutPct();
        boolean newer = v.getBuildNo() > clientBuild;

        data.put("hasUpdate", force || (newer && inRollout));
        data.put("force", force);
        data.put("version", v.getVersion());
        data.put("buildNo", v.getBuildNo());
        data.put("changelog", v.getChangelog());
        data.put("url", v.getUpgradeUrl());
        data.put("minBuild", v.getMinBuild());
        return data;
    }

    /** 新建/编辑：校验取值范围（状态机与数字边界）；id 为空新建 */
    public AppVersion saveVersion(AppVersion v, String operator) {
        String platform = v.getPlatform();
        if (!"ios".equals(platform) && !"android".equals(platform)) {
            throw new IllegalArgumentException("platform 仅支持 ios / android");
        }
        if (v.getBuildNo() == null || v.getBuildNo() <= 0) {
            throw new IllegalArgumentException("buildNo 必须为正整数");
        }
        if (v.getVersion() == null || v.getVersion().isBlank()) {
            throw new IllegalArgumentException("version 不能为空");
        }
        if (v.getRolloutPct() == null || v.getRolloutPct() < 0 || v.getRolloutPct() > 100) {
            v.setRolloutPct(v.getRolloutPct() == null ? 100 : Math.min(100, Math.max(0, v.getRolloutPct())));
        }
        if (v.getStatus() == null) {
            v.setStatus(0);
        }
        if (v.getStatus() != 0 && v.getStatus() != 1 && v.getStatus() != 2 && v.getStatus() != 3) {
            throw new IllegalArgumentException("status 非法（0 草稿 / 1 灰度中 / 2 全量 / 3 下线）");
        }
        // 发布中（1/2）必须有升级链接
        if ((v.getStatus() == 1 || v.getStatus() == 2)
                && (v.getUpgradeUrl() == null || v.getUpgradeUrl().isBlank())) {
            throw new IllegalArgumentException("发布中记录必须配置 upgradeUrl");
        }
        if (v.getId() == null) {
            v.setCreateBy(operator);
            v.setCreateTime(new Date());
            if (v.getStatus() == 1 || v.getStatus() == 2) {
                v.setPublishTime(new Date());
            }
            versionMapper.insert(v);
        } else {
            AppVersion old = versionMapper.selectById(v.getId());
            if (old == null) {
                throw new IllegalArgumentException("记录不存在");
            }
            // 从草稿转发布时补 publishTime
            if ((v.getStatus() == 1 || v.getStatus() == 2)
                    && (old.getStatus() == null || old.getStatus() == 0)) {
                v.setPublishTime(new Date());
            }
            versionMapper.updateById(v);
        }
        return v;
    }

    /** 回滚：将 platform 下发布中的记录全部下线，targetId 重新置为全量 */
    public void rollback(String platform, Long targetId) {
        AppVersion target = versionMapper.selectById(targetId);
        if (target == null || !platform.equals(target.getPlatform())) {
            throw new IllegalArgumentException("回滚目标不存在或平台不匹配");
        }
        List<AppVersion> online = versionMapper.selectList(new LambdaQueryWrapper<AppVersion>()
                .eq(AppVersion::getPlatform, platform)
                .in(AppVersion::getStatus, 1, 2));
        Date now = new Date();
        for (AppVersion v : online) {
            if (!v.getId().equals(targetId)) {
                versionMapper.updateById(new AppVersion().setId(v.getId()).setStatus(3));
            }
        }
        versionMapper.updateById(new AppVersion()
                .setId(targetId)
                .setStatus(2)
                .setPublishTime(now));
        log.info("[version] 回滚 platform={} targetId={} 版本={}", platform, targetId, target.getVersion());
    }

    /** 删除：仅草稿与已下线可删 */
    public void delete(Long id) {
        AppVersion v = versionMapper.selectById(id);
        if (v == null) {
            return;
        }
        if (v.getStatus() != null && (v.getStatus() == 1 || v.getStatus() == 2)) {
            throw new IllegalArgumentException("发布中的版本不可删除，请先下线");
        }
        versionMapper.deleteById(id);
    }
}
