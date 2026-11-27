package org.jeecg.modules.admin.ailing.tune.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 芥灵调优中心 · 词表维护 Mapper（P4，admin 模块自建 SQL，不依赖 app 模块只读实体——
 * app 端 KeywordAlias 无 id 字段且只读，写路径需完整行操作）。
 */
public interface AilingAliasAdminMapper {

    /** 词表分页：alias/standard 模糊 + 类型/启用筛选，update_time desc + LIMIT */
    @Select("<script>SELECT * FROM tbl_keyword_alias WHERE 1 = 1"
            + "<if test='alias != null and alias != \"\"'> AND alias LIKE concat('%',#{alias},'%')</if>"
            + "<if test='standard != null and standard != \"\"'> AND standard LIKE concat('%',#{standard},'%')</if>"
            + "<if test='aliasType != null'> AND alias_type = #{aliasType}</if>"
            + "<if test='enabled != null'> AND enabled = #{enabled}</if>"
            + " ORDER BY update_time DESC, id DESC LIMIT #{page.offset}, #{page.size}"
            + "</script>")
    List<Map<String, Object>> selectAliasPage(@Param("page") AilingTuneAdminMapper.PageArgsHolder page,
                                              @Param("alias") String alias, @Param("standard") String standard,
                                              @Param("aliasType") Integer aliasType, @Param("enabled") Integer enabled);

    @Select("<script>SELECT COUNT(*) FROM tbl_keyword_alias WHERE 1 = 1"
            + "<if test='alias != null and alias != \"\"'> AND alias LIKE concat('%',#{alias},'%')</if>"
            + "<if test='standard != null and standard != \"\"'> AND standard LIKE concat('%',#{standard},'%')</if>"
            + "<if test='aliasType != null'> AND alias_type = #{aliasType}</if>"
            + "<if test='enabled != null'> AND enabled = #{enabled}</if>"
            + "</script>")
    Long countAlias(@Param("alias") String alias, @Param("standard") String standard,
                    @Param("aliasType") Integer aliasType, @Param("enabled") Integer enabled);

    @Select("SELECT * FROM tbl_keyword_alias WHERE id = #{id}")
    Map<String, Object> selectAliasById(@Param("id") String id);

    /** uk_alias(alias,standard) 唯一冲突检查（排除自身） */
    @Select("<script>SELECT COUNT(*) FROM tbl_keyword_alias WHERE alias = #{alias} AND standard = #{standard}"
            + "<if test='excludeId != null and excludeId != \"\"'> AND id != #{excludeId}</if>"
            + "</script>")
    Long countAliasConflict(@Param("alias") String alias, @Param("standard") String standard,
                            @Param("excludeId") String excludeId);

    @Insert("INSERT INTO tbl_keyword_alias (id, alias, standard, alias_type, enabled, create_by, create_time, update_time) "
            + "VALUES (#{id}, #{alias}, #{standard}, #{aliasType}, #{enabled}, #{createBy}, NOW(), NOW())")
    int insertAlias(@Param("id") long id, @Param("alias") String alias, @Param("standard") String standard,
                    @Param("aliasType") Integer aliasType, @Param("enabled") Integer enabled,
                    @Param("createBy") String createBy);

    /** 更新：alias 不可改（改=删旧建新），仅 standard/词型/启用态 */
    @Update("UPDATE tbl_keyword_alias SET standard = #{standard}, alias_type = #{aliasType}, "
            + "enabled = #{enabled}, update_time = NOW() WHERE id = #{id}")
    int updateAlias(@Param("id") String id, @Param("standard") String standard,
                    @Param("aliasType") Integer aliasType, @Param("enabled") Integer enabled);

    @Delete("DELETE FROM tbl_keyword_alias WHERE id = #{id}")
    int deleteAlias(@Param("id") String id);

    /** 样本补词表（tbl_ai_feedback 处置动作）：与 save 同口径 */
    @Insert("INSERT INTO tbl_keyword_alias (id, alias, standard, alias_type, enabled, create_by, create_time, update_time) "
            + "VALUES (#{id}, #{alias}, #{standard}, #{aliasType}, 1, #{createBy}, NOW(), NOW())")
    int insertAliasFromSample(@Param("id") long id, @Param("alias") String alias, @Param("standard") String standard,
                              @Param("aliasType") Integer aliasType, @Param("createBy") String createBy);
}
