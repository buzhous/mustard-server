package org.jeecg.modules.admin.controller.article;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
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

import java.util.Date;
import java.util.List;

/**
 * 文章栏目 后台管理
 */
@Slf4j
@RestController
@RequestMapping("/article/admin/column")
@Tag(name = "文章栏目管理")
public class ArticleColumnAdminController {

    @Autowired
    private IArticleColumnService columnService;

    @Autowired
    private IArticleService articleService;

    @Operation(summary = "栏目列表（全部启用/停用，按排序返回）")
    @GetMapping(value = "/list")
    public Result<?> list() {
        QueryWrapper<ArticleColumn> queryWrapper = new QueryWrapper<ArticleColumn>()
                .eq("del_flag", 0)
                .orderByAsc("sort_no");
        return Result.ok(columnService.list(queryWrapper));
    }

    @Operation(summary = "新增栏目")
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody ArticleColumn column) {
        String msg = checkColumn(column);
        if (msg != null) {
            return Result.error(msg);
        }
        ArticleColumn exist = findByCode(column.getColumnCode());
        if (exist != null) {
            return Result.error("栏目编码已存在：" + column.getColumnCode());
        }
        column.setId(null);
        column.setCreateBy("admin");
        column.setCreateTime(new Date());
        column.setDelFlag(0);
        if (column.getStatus() == null) {
            column.setStatus(ArticleColumn.STATUS_ENABLED);
        }
        if (column.getSortNo() == null) {
            column.setSortNo(0);
        }
        columnService.save(column);
        return Result.ok("添加成功!");
    }

    @Operation(summary = "编辑栏目")
    @PostMapping(value = "/edit")
    public Result<?> edit(@RequestBody ArticleColumn column) {
        if (!StringUtils.hasText(column.getId())) {
            return Result.error("缺少栏目 id");
        }
        if (columnService.getById(column.getId()) == null) {
            return Result.error("未找到对应数据");
        }
        String msg = checkColumn(column);
        if (msg != null) {
            return Result.error(msg);
        }
        ArticleColumn exist = findByCode(column.getColumnCode());
        if (exist != null && !exist.getId().equals(column.getId())) {
            return Result.error("栏目编码已存在：" + column.getColumnCode());
        }
        column.setUpdateBy("admin");
        column.setUpdateTime(new Date());
        columnService.updateById(column);
        return Result.ok("编辑成功!");
    }

    @Operation(summary = "通过id删除栏目（栏目下存在文章时不可删除）")
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        long articleCount = articleService.count(
                new QueryWrapper<Article>().eq("column_id", id).eq("del_flag", 0));
        if (articleCount > 0) {
            return Result.error("该栏目下存在 " + articleCount + " 篇文章，请先删除或移出文章");
        }
        columnService.removeById(id);
        return Result.ok("删除成功!");
    }

    private String checkColumn(ArticleColumn column) {
        if (!StringUtils.hasText(column.getColumnName())) {
            return "请填写栏目名称";
        }
        if (column.getColumnName().length() > 50) {
            return "栏目名称不能超过 50 字";
        }
        if (!StringUtils.hasText(column.getColumnCode())) {
            return "请填写栏目编码";
        }
        if (!column.getColumnCode().matches("[a-zA-Z][a-zA-Z0-9_]{1,49}")) {
            return "栏目编码须以字母开头，仅含字母/数字/下划线，长度 2-50";
        }
        return null;
    }

    private ArticleColumn findByCode(String columnCode) {
        if (!StringUtils.hasText(columnCode)) {
            return null;
        }
        return columnService.getOne(
                new QueryWrapper<ArticleColumn>().eq("column_code", columnCode.trim()).eq("del_flag", 0).last("LIMIT 1"));
    }
}
