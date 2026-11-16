package org.jeecg.modules.app.entity.feedback;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户反馈（表 tbl_feedback）——App 提交侧实体
 * 后台管理侧实体在 jeecg-module-admin（两模块互不依赖，共用同一张表）
 */
@Data
@TableName("tbl_feedback")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
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
    private String id;

    private Date createTime;

    private Date updateTime;

    /**
     * 提交用户
     */
    private String userId;

    /**
     * 联系方式（选填）
     */
    private String contact;

    /**
     * 反馈内容
     */
    private String content;

    /**
     * 运营回复
     */
    private String reply;

    /**
     * 状态 0-待处理 1-已处理
     */
    private Integer status;
}
