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
 * 芥子开放平台应用凭据
 * 第三方应用凭 AK/SK 调用 /app/open/** 接口，数据范围限定为 bound_uid 对应用户的物品数据。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("open_app_credentials")
@Schema(description = "开放平台应用凭据")
public class OpenAppCredential implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private String id;

    @Schema(description = "应用AppKey")
    private String appKey;

    @Schema(description = "应用密钥AppSecret")
    private String appSecret;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "数据范围绑定的App用户ID")
    private String boundUid;

    @Schema(description = "权限分级：readonly/readwrite（默认 readonly）")
    private String scope;

    @Schema(description = "日调用配额，NULL=默认值（默认 5000）")
    private Integer dailyQuota;

    @Schema(description = "scope 最近变更时间")
    private Date scopeUpdatedAt;

    @Schema(description = "Webhook 回调地址（空为未订阅）")
    private String webhookUrl;

    @Schema(description = "Webhook 签名密钥（HMAC-SHA256）")
    private String webhookSecret;

    @Schema(description = "状态：0停用 1启用")
    private Integer status;

    @Schema(description = "IP白名单，逗号分隔，空为不限制")
    private String whiteList;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;
}
