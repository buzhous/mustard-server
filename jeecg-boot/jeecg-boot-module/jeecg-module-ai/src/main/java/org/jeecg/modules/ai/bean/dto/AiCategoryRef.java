package org.jeecg.modules.ai.bean.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分类引用（ai 模块自建 DTO，字段为编排器/查询管线实际读取的分类字段）
 * <p>
 * 统一分类清单语义（C3 分类治理）：
 * - 用户分类：id=用户分类id、user=true、userCategoryId=id
 * - 系统分类（未实例化）：id=系统分类id、system=true、userCategoryId=null（物品按用户分类id挂载，AI 仅可读名）
 */
@Data
@Accessors(chain = true)
public class AiCategoryRef implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;

    private String name;

    /** 用户分类（false=系统分类） */
    private boolean user;

    /** 系统分类对应的用户分类 id（已实例化时非空；用户分类恒等于 id） */
    private String userCategoryId;
}
