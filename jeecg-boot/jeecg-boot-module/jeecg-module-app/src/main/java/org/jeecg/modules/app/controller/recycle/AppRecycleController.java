package org.jeecg.modules.app.controller.recycle;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.ruins.ItemRuinsListVO;
import org.jeecg.modules.app.entity.user.AppUser;
import org.jeecg.modules.app.service.IItemRecycleService;
import org.jeecg.modules.app.utils.AppAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 回收站（个人中心-设置）：用户独立回收站（与殷墟隔离），本人删除的物品保留 30 天，
 * 30 天内可恢复（撤回删除）或彻底删除，超期由定时任务清理
 */
@Tag(name = "回收站")
@Slf4j
@RestController
@RequestMapping("/app/recycle")
public class AppRecycleController {

    @Autowired
    private IItemRecycleService itemRecycleService;

    @Operation(summary = "回收站分页列表（本人删除、30天内）")
    @GetMapping("/list")
    public Result<IPage<ItemRuinsListVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            throw new AppException(ExceptionEnum.USER_INFO_NOT_EXIST);
        }
        IPage<ItemRuinsListVO> recycleIPage = itemRecycleService.queryMyRecyclePage(page, size, loginUser.getId());
        return Result.OK(recycleIPage);
    }

    @Operation(summary = "恢复（撤回删除，物品回库存）")
    @PostMapping("/restore")
    public Result<String> restore(@RequestParam String ruinsId, @RequestParam String itemId) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            throw new AppException(ExceptionEnum.USER_INFO_NOT_EXIST);
        }
        itemRecycleService.restoreFromRecycle(loginUser.getId(), ruinsId, itemId);
        return Result.OK("恢复成功");
    }

    @Operation(summary = "彻底删除（物理删除，不可恢复）")
    @PostMapping("/purge")
    public Result<String> purge(@RequestParam String ruinsId, @RequestParam String itemId) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            throw new AppException(ExceptionEnum.USER_INFO_NOT_EXIST);
        }
        itemRecycleService.purgeRecycle(loginUser.getId(), ruinsId, itemId);
        return Result.OK("已彻底删除");
    }

    @Operation(summary = "清除全部（清空本人回收站，不可恢复）")
    @PostMapping("/purgeAll")
    public Result<String> purgeAll() {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            throw new AppException(ExceptionEnum.USER_INFO_NOT_EXIST);
        }
        int count = itemRecycleService.purgeAllRecycle(loginUser.getId());
        return Result.OK("已清除 " + count + " 条记录");
    }

}
