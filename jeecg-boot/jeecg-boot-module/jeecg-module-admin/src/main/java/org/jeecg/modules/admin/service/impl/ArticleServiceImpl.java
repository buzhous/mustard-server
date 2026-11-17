package org.jeecg.modules.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.admin.entity.article.Article;
import org.jeecg.modules.admin.mapper.ArticleMapper;
import org.jeecg.modules.admin.service.IArticleService;
import org.springframework.stereotype.Service;

/**
 * 文章 服务实现
 */
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements IArticleService {
}
