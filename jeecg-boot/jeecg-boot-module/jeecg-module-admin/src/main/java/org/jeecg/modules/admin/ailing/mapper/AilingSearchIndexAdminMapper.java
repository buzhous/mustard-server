package org.jeecg.modules.admin.ailing.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 芥灵AI 物品工作台 · 检索索引只读 Mapper（全量重建迭代用）
 */
public interface AilingSearchIndexAdminMapper {

    @Select("SELECT id FROM tbl_item_info WHERE id > #{lastId} ORDER BY id ASC LIMIT #{limit}")
    List<Long> selectItemIdsAfter(@Param("lastId") long lastId, @Param("limit") int limit);

    @Select("SELECT count(*) FROM tbl_item_search_index")
    Long countIndexRows();
}
