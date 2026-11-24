package org.jeecg.modules.app.controller.manage;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.entity.SystemCategory;
import org.jeecg.modules.app.service.ISystemCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import cn.hutool.core.util.StrUtil;

/**
 * 管理后台：系统物品分类治理（C3 分类治理，运营增/停/改）
 * <p>
 * 鉴权依赖 jeecg 全局 JwtFilter（非 /app/** 路径走管理端 token，X-Access-Token）；
 * 上线建议在权限体系为 /manage/system/category 配置专属权限点后再放开运营使用。
 * <p>
 * 约束：名称唯一（uk_system_category_name）；停用（status=0）不物理删除——
 * 已实例化的 tbl_user_category.system_id 仍指向该行，保留聚合关系。
 */
@Slf4j
@RestController
@RequestMapping("/manage/system/category")
@Tag(name = "系统分类管理（管理后台）")
public class SystemCategoryManageController {

    /** 系统分类启用状态（1启用 0停用） */
    private static final int SYS_CATEGORY_ENABLED = 1;

    @Autowired
    private ISystemCategoryService systemCategoryService;

    @Operation(summary = "系统分类分页列表")
    @GetMapping("/list")
    public Result<?> list(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                          @RequestParam(name = "pageSize", defaultValue = "50") Integer pageSize,
                          @RequestParam(name = "name", required = false) String name) {
        LambdaQueryWrapper<SystemCategory> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(name)) {
            wrapper.like(SystemCategory::getName, name.trim());
        }
        wrapper.orderByAsc(SystemCategory::getSort);
        IPage<SystemCategory> page = systemCategoryService.page(new Page<>(pageNo, Math.min(pageSize, 200)), wrapper);
        return Result.OK(page);
    }

    @Operation(summary = "新增系统分类")
    @PostMapping("/add")
    public Result<?> add(@RequestBody SystemCategory category) {
        category.setId(null);
        if (StrUtil.isBlank(category.getName())) {
            return Result.error(ExceptionEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        category.setName(category.getName().trim());
        if (category.getStatus() == null) {
            category.setStatus(SYS_CATEGORY_ENABLED);
        }
        // 名称唯一校验（uk_system_category_name 兜底）
        long sameName = systemCategoryService.count(new LambdaQueryWrapper<SystemCategory>()
                .eq(SystemCategory::getName, category.getName()));
        if (sameName > 0) {
            return Result.error("系统分类名称已存在");
        }
        systemCategoryService.save(category);
        return Result.OK("新增成功", category.getId());
    }

    @Operation(summary = "编辑系统分类（名称/图标/排序/状态/说明）")
    @PostMapping("/edit")
    public Result<?> edit(@RequestBody SystemCategory category) {
        if (StrUtil.isBlank(category.getId())) {
            return Result.error(ExceptionEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        SystemCategory exist = systemCategoryService.getById(category.getId());
        if (exist == null) {
            return Result.error(ExceptionEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        if (StrUtil.isNotBlank(category.getName()) && !category.getName().trim().equals(exist.getName())) {
            long sameName = systemCategoryService.count(new LambdaQueryWrapper<SystemCategory>()
                    .eq(SystemCategory::getName, category.getName().trim())
                    .ne(SystemCategory::getId, category.getId()));
            if (sameName > 0) {
                return Result.error("系统分类名称已存在");
            }
            category.setName(category.getName().trim());
        }
        systemCategoryService.updateById(category);
        return Result.OK("编辑成功", category.getId());
    }

    @Operation(summary = "停用/启用系统分类（status=0 停用 1 启用；建议停用而非删除）")
    @PostMapping("/status")
    public Result<?> changeStatus(@RequestParam(name = "id") String id,
                                  @RequestParam(name = "status") Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return Result.error(ExceptionEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        SystemCategory update = new SystemCategory();
        update.setId(id);
        update.setStatus(status);
        boolean ok = systemCategoryService.updateById(update);
        return ok ? Result.OK("操作成功") : Result.error(ExceptionEnum.DATA_UPDATE_ERROR.getMsg());
    }
}
