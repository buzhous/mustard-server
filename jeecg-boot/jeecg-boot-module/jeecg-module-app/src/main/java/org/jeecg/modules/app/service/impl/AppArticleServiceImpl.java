package org.jeecg.modules.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.app.entity.article.Article;
import org.jeecg.modules.app.entity.article.ArticleColumn;
import org.jeecg.modules.app.mapper.AppArticleColumnMapper;
import org.jeecg.modules.app.mapper.AppArticleMapper;
import org.jeecg.modules.app.service.IAppArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文章 服务实现（App 端只读）
 */
@Service
public class AppArticleServiceImpl extends ServiceImpl<AppArticleMapper, Article> implements IAppArticleService {

    @Autowired
    private AppArticleColumnMapper columnMapper;

    @Override
    public List<ArticleColumn> listEnabledColumns() {
        return columnMapper.selectList(new QueryWrapper<ArticleColumn>()
                .eq("status", ArticleColumn.STATUS_ENABLED)
                .eq("del_flag", 0)
                .orderByAsc("sort_no"));
    }

    @Override
    public List<Article> listPublishedByColumnCode(String columnCode) {
        QueryWrapper<Article> queryWrapper = new QueryWrapper<Article>()
                .eq("status", Article.STATUS_PUBLISHED)
                .eq("del_flag", 0)
                .orderByAsc("sort_no")
                .orderByDesc("publish_time");
        if (StringUtils.isNotBlank(columnCode)) {
            queryWrapper.eq("column_code", columnCode.trim());
        }
        return list(queryWrapper);
    }

    @Override
    public Article getPublishedDetail(String articleCode, String id) {
        QueryWrapper<Article> queryWrapper = new QueryWrapper<Article>()
                .eq("status", Article.STATUS_PUBLISHED)
                .eq("del_flag", 0)
                .last("LIMIT 1");
        if (StringUtils.isNotBlank(articleCode)) {
            queryWrapper.eq("article_code", articleCode.trim());
        } else if (StringUtils.isNotBlank(id)) {
            queryWrapper.eq("id", id.trim());
        } else {
            return null;
        }
        return getOne(queryWrapper);
    }
}
