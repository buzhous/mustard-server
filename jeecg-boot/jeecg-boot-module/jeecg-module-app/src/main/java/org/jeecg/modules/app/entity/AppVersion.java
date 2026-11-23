package org.jeecg.modules.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * App 版本管理（版本管理策略 §4.1）：双平台各自维护 latest，强更门禁独立生效。
 * 状态机：0 草稿 → 1 灰度中 → 2 全量 → 3 下线（回滚 = 历史 targetId 重新置 2）。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("tbl_app_version")
@Schema(description = "App 版本发布记录")
public class AppVersion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "平台 ios / android")
    private String platform;

    @Schema(description = "版本字符串（如 1.9.9，双平台统一）")
    private String version;

    @Schema(description = "构建号（分平台独立递增；latest 判定基准）")
    private Integer buildNo;

    @Schema(description = "强更门禁线：低于此 build 强制更新（空 = 不设门禁）")
    private Integer minBuild;

    @Schema(description = "1=门禁以下强更；0=仅提醒")
    private Integer forceUpgrade;

    @Schema(description = "灰度百分比 0-100（deviceId hash 稳定分桶；强更不受灰度限制）")
    private Integer rolloutPct;

    @Schema(description = "0=草稿 1=灰度中 2=全量 3=下线")
    private Integer status;

    @Schema(description = "升级链接（商店页 / APK 直链）")
    private String upgradeUrl;

    @Schema(description = "Android 直链专用：APK MD5 完整性校验")
    private String apkMd5;

    @Schema(description = "Android 直链专用：APK 字节数（下载预检与进度总量）")
    private Long apkSize;

    @Schema(description = "更新说明（弹窗展示）")
    private String changelog;

    @Schema(description = "发布时间（latest 取数基准）")
    private Date publishTime;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private Date createTime;
}
