package org.jeecg.modules.admin.controller.article;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.admin.entity.article.Article;
import org.jeecg.modules.admin.entity.article.ArticleColumn;
import org.jeecg.modules.admin.service.IArticleColumnService;
import org.jeecg.modules.admin.service.IArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文章管理 后台 CRUD
 */
@Slf4j
@RestController
@RequestMapping("/article/admin")
@Tag(name = "文章管理")
public class ArticleAdminController extends JeecgController<Article, IArticleService> {

    @Autowired
    private IArticleService articleService;

    @Autowired
    private IArticleColumnService columnService;

    @Operation(summary = "文章分页列表（支持标题/栏目/状态筛选）")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(Article article,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<Article> queryWrapper = QueryGenerator.initQueryWrapper(article, req.getParameterMap());
        queryWrapper.orderByAsc("sort_no").orderByDesc("create_time");
        Page<Article> page = new Page<>(pageNo, pageSize);
        IPage<Article> pageList = articleService.page(page, queryWrapper);
        List<Article> records = pageList.getRecords();
        // 列表不回传大字段富文本，详情走 queryById
        records.forEach(r -> r.setContent(null));
        fillColumnNames(records);
        return Result.ok(pageList);
    }

    @Operation(summary = "文章详情（含富文本内容）")
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id") String id) {
        Article record = articleService.getById(id);
        if (record == null) {
            return Result.error("未找到对应数据");
        }
        fillColumnNames(Collections.singletonList(record));
        return Result.ok(record);
    }

    @Operation(summary = "新增文章")
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody Article article) {
        String checkMsg = checkArticle(article, true);
        if (checkMsg != null) {
            return Result.error(checkMsg);
        }
        Article exist = findByCode(article.getArticleCode());
        if (exist != null) {
            return Result.error("文章编码已存在：" + article.getArticleCode());
        }
        article.setId(null);
        article.setCreateBy("admin");
        article.setCreateTime(new Date());
        article.setDelFlag(0);
        if (article.getStatus() == null) {
            article.setStatus(Article.STATUS_PUBLISHED);
        }
        if (article.getStatus() == Article.STATUS_PUBLISHED && article.getPublishTime() == null) {
            article.setPublishTime(new Date());
        }
        fillColumnCode(article);
        articleService.save(article);
        return Result.ok("添加成功!");
    }

    @Operation(summary = "编辑文章")
    @PostMapping(value = "/edit")
    public Result<?> edit(@RequestBody Article article) {
        if (!StringUtils.hasText(article.getId())) {
            return Result.error("缺少文章 id");
        }
        Article old = articleService.getById(article.getId());
        if (old == null) {
            return Result.error("未找到对应数据");
        }
        String checkMsg = checkArticle(article, false);
        if (checkMsg != null) {
            return Result.error(checkMsg);
        }
        Article exist = findByCode(article.getArticleCode());
        if (exist != null && !exist.getId().equals(article.getId())) {
            return Result.error("文章编码已存在：" + article.getArticleCode());
        }
        article.setUpdateBy("admin");
        article.setUpdateTime(new Date());
        if (article.getStatus() != null && article.getStatus() == Article.STATUS_PUBLISHED && old.getPublishTime() == null) {
            article.setPublishTime(new Date());
        }
        fillColumnCode(article);
        articleService.updateById(article);
        return Result.ok("编辑成功!");
    }

    @Operation(summary = "通过id删除文章")
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        articleService.removeById(id);
        return Result.ok("删除成功!");
    }

    @Operation(summary = "批量删除文章")
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids") String ids) {
        List<String> idList = splitIds(ids);
        if (idList.isEmpty()) {
            return Result.error("请选择要删除的数据");
        }
        articleService.removeByIds(idList);
        return Result.ok("批量删除成功!");
    }

    private String checkArticle(Article article, boolean forAdd) {
        if (forAdd) {
            if (!StringUtils.hasText(article.getColumnId())) {
                return "请选择所属栏目";
            }
            if (!StringUtils.hasText(article.getTitle())) {
                return "请填写文章标题";
            }
            if (!StringUtils.hasText(article.getContent())) {
                return "请填写文章内容";
            }
            if (article.getTitle().length() > 100) {
                return "文章标题不能超过 100 字";
            }
        } else {
            // 编辑为局部更新，仅校验传入字段
            if (article.getTitle() != null && article.getTitle().length() > 100) {
                return "文章标题不能超过 100 字";
            }
        }
        return null;
    }

    private Article findByCode(String articleCode) {
        if (!StringUtils.hasText(articleCode)) {
            return null;
        }
        return articleService.getOne(
                new QueryWrapper<Article>().eq("article_code", articleCode.trim()).eq("del_flag", 0).last("LIMIT 1"));
    }

    private void fillColumnCode(Article article) {
        if (StringUtils.hasText(article.getColumnId())) {
            ArticleColumn column = columnService.getById(article.getColumnId());
            if (column != null) {
                article.setColumnCode(column.getColumnCode());
            }
        }
    }

    private void fillColumnNames(List<Article> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<String, String> nameMap = columnService.list().stream()
                .collect(Collectors.toMap(ArticleColumn::getId, ArticleColumn::getColumnName, (a, b) -> a));
        records.forEach(r -> r.setColumnName(nameMap.get(r.getColumnId())));
    }

    private List<String> splitIds(String ids) {
        if (!StringUtils.hasText(ids)) {
            return Collections.emptyList();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
