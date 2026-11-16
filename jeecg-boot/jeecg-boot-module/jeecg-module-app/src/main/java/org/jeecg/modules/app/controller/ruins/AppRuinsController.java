package org.jeecg.modules.app.controller.ruins;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.ruins.ItemRuinsListVO;
import org.jeecg.modules.app.entity.ItemSync;
import org.jeecg.modules.app.entity.user.AppUser;
import org.jeecg.modules.app.service.IItemRuinsService;
import org.jeecg.modules.app.utils.AppAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "废墟广场")
@Slf4j
@RestController
@RequestMapping("/app/ruins")
public class AppRuinsController {

    @Autowired
    private IItemRuinsService itemRuinsService;


    @Operation(summary = "废墟列表")
    @GetMapping("/list")
    public Result<IPage<ItemRuinsListVO>> list(
            @RequestParam(required = false) String searchKey,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            throw new AppException(ExceptionEnum.USER_INFO_NOT_EXIST);
        }
        IPage<ItemRuinsListVO> ruinsIPage = itemRuinsService.queryNotPickupRuinsPage(page, size, loginUser.getId());
        return Result.OK(ruinsIPage);
    }

    @Operation(summary = "拾取（消耗一张殷墟拾取券，返回拾取者名下物品信息）")
    @GetMapping("/pickup")
    public Result<ItemSync> pickup(@RequestParam String itemId, @RequestParam String ruinsId) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            throw new AppException(ExceptionEnum.USER_INFO_NOT_EXIST);
        }

        ItemSync itemSync = itemRuinsService.pickup(ruinsId, itemId, loginUser.getId());
        return Result.OK(itemSync);
    }

}