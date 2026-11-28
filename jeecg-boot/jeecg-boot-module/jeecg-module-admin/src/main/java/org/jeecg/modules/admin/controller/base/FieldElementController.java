package org.jeecg.modules.admin.controller.base;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.entity.FieldElement;
import org.jeecg.modules.app.service.IBaseFieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 字段字典管理（基础配置 > 字段管理，后台管理）
 * <p>
 * 薄层转发独立基础服务 {@link IBaseFieldService}；读路径走 service 缓存（不直查 DB）。
 * 约束：已录入字段一律不可删除（无 delete 接口），仅支持停用/启用；
 * 编辑时组件编号锁定、必填三要素（1 图标 / 2 名称 / 3 类别）锁定必填属性；
 * 所有写操作经 service 落库后自动刷新缓存、版本 +1、续期 365 天。
 */
@Slf4j
@RestController
@RequestMapping("/admin/fieldElement")
@Tag(name = "字段字典管理")
public class FieldElementController {

    @Autowired
    private IBaseFieldService baseFieldService;

    @Operation(summary = "字段字典分页列表（筛选组件编号/名称/输入类型/端标记/状态，按 sort 升序；读缓存）")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   @RequestParam(name = "element", required = false) Integer element,
                                   @RequestParam(name = "name", required = false) String name,
                                   @RequestParam(name = "inputType", required = false) String inputType,
                                   @RequestParam(name = "androidFlag", required = false) Integer androidFlag,
                                   @RequestParam(name = "iosFlag", required = false) Integer iosFlag,
                                   @RequestParam(name = "status", required = false) Integer status) {
        List<FieldElement> filtered = baseFieldService.getAllFields().stream()
                .filter(f -> element == null || element.equals(f.getElement()))
                .filter(f -> name == null || name.isBlank() || (f.getName() != null && f.getName().contains(name.trim())))
                .filter(f -> inputType == null || inputType.isBlank() || inputType.equals(f.getInputType()))
                .filter(f -> androidFlag == null || androidFlag.equals(f.getAndroidFlag()))
                .filter(f -> iosFlag == null || iosFlag.equals(f.getIosFlag()))
                .filter(f -> status == null || status.equals(f.getStatus()))
                .toList();

        int pageSizeVal = Math.max(Math.min(pageSize, 200), 1);
        int total = filtered.size();
        long fromL = (long) (Math.max(pageNo, 1) - 1) * pageSizeVal;
        int from = (int) Math.min(fromL, total);
        int to = (int) Math.min(fromL + pageSizeVal, total);

        Page<FieldElement> page = new Page<>(pageNo, pageSizeVal, total);
        page.setRecords(from >= to ? List.of() : filtered.subList(from, to));
        return Result.OK(page);
    }

    @Operation(summary = "字段详情（抽屉编辑回填）")
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id") String id) {
        FieldElement field = baseFieldService.getById(id);
        if (field == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(field);
    }

    @Operation(summary = "新增字段（扩展编号可自动顺延分配）→ service 校验 + 写库 + 刷缓存")
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody FieldElement fieldElement) {
        try {
            return Result.OK("添加成功！", baseFieldService.saveElement(fieldElement));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "编辑字段（组件编号锁定）→ service 校验 + 写库 + 刷缓存")
    @PutMapping(value = "/edit")
    public Result<?> edit(@RequestBody FieldElement fieldElement) {
        if (fieldElement == null || fieldElement.getId() == null) {
            return Result.error("缺少主键id");
        }
        try {
            return Result.OK("更新成功！", baseFieldService.saveElement(fieldElement));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "启用/停用字段（不提供删除）→ 状态变更后刷新缓存")
    @PostMapping(value = "/status")
    public Result<?> changeStatus(@RequestParam(name = "id") String id,
                                  @RequestParam(name = "status") Integer status) {
        try {
            baseFieldService.toggleStatus(id, status);
            return Result.OK("操作成功！");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
