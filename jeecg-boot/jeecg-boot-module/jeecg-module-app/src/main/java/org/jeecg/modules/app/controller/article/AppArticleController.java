package org.jeecg.modules.app.controller.article;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.entity.article.Article;
import org.jeecg.modules.app.service.IAppArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文章（App 端：协议等文章获取，登录前即可访问，无需鉴权）
 */
@Slf4j
@RestController
@RequestMapping("/app/article")
@Tag(name = "App-文章")
public class AppArticleController {

    @Autowired
    private IAppArticleService articleService;

    @Operation(summary = "启用栏目列表")
    @GetMapping("/columns")
    public Result<?> columns() {
        return Result.ok(articleService.listEnabledColumns());
    }

    @Operation(summary = "栏目下已发布文章列表（不含正文，columnCode 选填）")
    @GetMapping("/list")
    public Result<?> list(@RequestParam(name = "columnCode", required = false) String columnCode) {
        List<Article> articles = articleService.listPublishedByColumnCode(columnCode);
        articles.forEach(a -> a.setContent(null));
        return Result.ok(articles);
    }

    @Operation(summary = "文章详情（含富文本，code 优先，其次 id）")
    @GetMapping("/detail")
    public Result<?> detail(@RequestParam(name = "code", required = false) String code,
                            @RequestParam(name = "id", required = false) String id) {
        Article article = articleService.getPublishedDetail(code, id);
        if (article == null) {
            return Result.error("文章不存在或已下架");
        }
        return Result.ok(article);
    }
}
