package org.jeecg.modules.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.app.entity.article.Article;
import org.jeecg.modules.app.entity.article.ArticleColumn;

import java.util.List;

/**
 * 文章 服务接口（App 端获取协议/文章内容）
 */
public interface IAppArticleService extends IService<Article> {

    /**
     * 查询启用的栏目列表（按排序）
     */
    List<ArticleColumn> listEnabledColumns();

    /**
     * 按栏目编码查询已发布文章（不含正文，按排序）
     */
    List<Article> listPublishedByColumnCode(String columnCode);

    /**
     * 查询已发布文章详情：优先按编码，其次按 id
     */
    Article getPublishedDetail(String articleCode, String id);
}
