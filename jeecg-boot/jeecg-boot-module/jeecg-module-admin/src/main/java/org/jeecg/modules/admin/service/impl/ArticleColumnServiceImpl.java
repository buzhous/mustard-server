package org.jeecg.modules.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.admin.entity.article.ArticleColumn;
import org.jeecg.modules.admin.mapper.ArticleColumnMapper;
import org.jeecg.modules.admin.service.IArticleColumnService;
import org.springframework.stereotype.Service;

/**
 * 文章栏目 服务实现
 */
@Service
public class ArticleColumnServiceImpl extends ServiceImpl<ArticleColumnMapper, ArticleColumn> implements IArticleColumnService {
}
