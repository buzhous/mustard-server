package org.jeecg.modules.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * AI 反馈样本（tbl_ai_feedback，P4）：App 用户反馈（source_type=1）与
 * 观测低命中样本自动入库（source_type=2）共表。
 */
@Data
@TableName("tbl_ai_feedback")
@Accessors(chain = true)
public class AiFeedback implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 来源:1用户反馈,2观测低命中,3人工标记 */
    private Integer sourceType;

    private String sessionId;

    private String messageId;

    private String uid;

    /** 关联对象:1查询,2草稿 */
    private Integer refType;

    /** 关联内容(查询词/草稿摘要) */
    private String refContent;

    /** 结果摘要(命中数/命中来源) */
    private String resultSummary;

    /** 反馈原因 */
    private String reason;

    /** 处理状态:0待处理,1已入库,2已忽略 */
    private Integer sampleStatus;

    private Date createTime;
}
