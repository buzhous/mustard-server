package org.jeecg.modules.app.controller.user;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.vo.category.SystemCategoryVO;
import org.jeecg.modules.app.bean.vo.category.UserCategoryVO;
import org.jeecg.modules.app.entity.user.AppUser;
import org.jeecg.modules.app.service.ISystemCategoryService;
import org.jeecg.modules.app.service.IUserCategoryService;
import org.jeecg.modules.app.utils.AppAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * App 端系统分类（C3 分类治理）：
 * 列表（带当前用户「已关联」标记）+ 一键添加关联到用户分类（以系统分类为准）。
 */
@Slf4j
@RestController
@RequestMapping("/app/system/category")
@Tag(name = "系统分类")
public class AppSystemCategoryController {

    @Autowired
    private ISystemCategoryService systemCategoryService;

    @Autowired
    private IUserCategoryService userCategoryService;

    @Operation(summary = "系统分类列表（带已关联标记）")
    @GetMapping("/list")
    public Result<?> list() {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error(ExceptionEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        List<SystemCategoryVO> categories = systemCategoryService.listForUser(loginUser.getId());
        return Result.OK(categories);
    }

    @Operation(summary = "添加系统分类关联到用户分类（重名以系统分类为准）")
    @PostMapping("/add")
    public Result<?> add(@RequestParam(name = "systemCategoryId") String systemCategoryId) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error(ExceptionEnum.USER_INFO_NOT_EXIST.getMsg());
        }
        if (StrUtil.isBlank(systemCategoryId)) {
            return Result.error(ExceptionEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        UserCategoryVO created = userCategoryService.addBySystemCategory(loginUser.getId(), systemCategoryId);
        if (created == null) {
            return Result.error(ExceptionEnum.REQUEST_PARAM_ERROR.getMsg());
        }
        return Result.OK("添加成功", created.getId());
    }

}
