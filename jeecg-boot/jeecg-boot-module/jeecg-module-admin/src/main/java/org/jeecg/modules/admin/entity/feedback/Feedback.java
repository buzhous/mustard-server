package org.jeecg.modules.admin.entity.feedback;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户反馈（表 tbl_feedback）
 */
@Data
@TableName("tbl_feedback")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户反馈")
public class Feedback implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 反馈状态-待处理
     */
    public static final int STATUS_PENDING = 0;

    /**
     * 反馈状态-已处理
     */
    public static final int STATUS_RESOLVED = 1;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private String id;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "提交时间")
    private java.util.Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "处理时间")
    private java.util.Date updateTime;

    @Schema(description = "提交用户")
    private String userId;

    @Schema(description = "联系方式（选填）")
    private String contact;

    @Schema(description = "反馈内容")
    private String content;

    @Schema(description = "运营回复")
    private String reply;

    @Schema(description = "状态 0-待处理 1-已处理")
    private Integer status;

    @TableField(exist = false)
    @Schema(description = "提交用户昵称（列表联查展示）")
    private String nickname;

    @TableField(exist = false)
    @Schema(description = "提交用户手机号（列表联查展示）")
    private String phone;
}
