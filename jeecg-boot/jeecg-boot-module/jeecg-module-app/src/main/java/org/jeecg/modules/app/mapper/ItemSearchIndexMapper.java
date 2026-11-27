package org.jeecg.modules.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.app.entity.ItemSearchIndex;

import java.util.List;

public interface ItemSearchIndexMapper extends BaseMapper<ItemSearchIndex> {

    /**
     * 全文检索命中的物品 id（ngram 自然语言模式：terms 任一分词命中即返回，相关度降序）
     */
    @Select("SELECT item_id FROM tbl_item_search_index WHERE uid = #{uid} "
            + "AND MATCH(search_blob) AGAINST(#{terms} IN NATURAL LANGUAGE MODE) LIMIT 200")
    List<String> selectIdsByTerms(@Param("uid") String uid, @Param("terms") String terms);
}
