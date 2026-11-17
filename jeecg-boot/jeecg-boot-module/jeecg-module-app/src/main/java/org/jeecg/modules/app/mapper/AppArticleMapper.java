package org.jeecg.modules.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jeecg.modules.app.entity.article.Article;

/**
 * 文章 Mapper（App 只读）
 */
@Mapper
public interface AppArticleMapper extends BaseMapper<Article> {
}
