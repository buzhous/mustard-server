package org.jeecg.modules.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
 * 字段字典（App / 服务端 / 后台三端共用的唯一字段数据源）：
 * 必填三要素 element 1 图标 / 2 名称 / 3 类别；内置种子 34 条，运营可扩展（35 起顺延分配）；
 * 已录入字段一律不可删除，仅支持停用/启用；android_flag / ios_flag 为端实现标记，
 * 未勾选端的字段该端 App 初始化不加载。
 */
@Data
@TableName("tbl_field_element")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "字段字典")
public class FieldElement implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private String id;

    @Schema(description = "组件编号，全局唯一（内置 1-34，扩展 35+）")
    private Integer element;

    @Schema(description = "字段名称（中文）")
    private String name;

    @Schema(description = "字段标识（英文驼峰，与物品 fields JSON 的 key 对应）")
    private String fieldKey;

    @Schema(description = "输入类型（内置 11 种可扩展，未知类型 App 按文本兜底渲染）")
    private String inputType;

    @Schema(description = "是否必填：1是 0否")
    private Integer isRequired;

    @Schema(description = "默认排序（App 表单/详情渲染顺序，扩展字段排末尾）")
    private Integer sort;

    @Schema(description = "安卓端实现标记：1已实现 0未实现")
    private Integer androidFlag;

    @Schema(description = "iOS 端实现标记：1已实现 0未实现")
    private Integer iosFlag;

    @Schema(description = "状态：1启用 0停用（不提供删除）")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private java.util.Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private java.util.Date updateTime;
}
