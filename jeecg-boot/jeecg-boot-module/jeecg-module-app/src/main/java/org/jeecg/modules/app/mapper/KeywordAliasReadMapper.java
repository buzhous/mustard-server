package org.jeecg.modules.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.app.entity.KeywordAlias;

import java.util.List;

/**
 * 别名词表只读 Mapper：仅提供启用词读取（缓存加载/别名扩展），
 * 维护操作（P4 后台页）不经此接口。
 */
public interface KeywordAliasReadMapper extends BaseMapper<KeywordAlias> {

    @Select("SELECT alias, standard, alias_type AS aliasType, enabled FROM tbl_keyword_alias WHERE enabled = 1")
    List<KeywordAlias> selectEnabled();
}
