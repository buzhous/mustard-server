package org.jeecg.modules.app.controller.vip;

import cn.hutool.core.util.ObjectUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.vip.VipInfoVO;
import org.jeecg.modules.app.bean.vo.vip.VipPurchaseReqVO;
import org.jeecg.modules.app.bean.vo.vip.VipPurchaseRespVO;
import org.jeecg.modules.app.entity.user.AppUser;
import org.jeecg.modules.app.service.IVipOrderService;
import org.jeecg.modules.app.utils.AppAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * VIP 会员（设置中心购买入口）
 * 99 元/年；当前默认模拟支付直接成功，预留微信/支付宝支付接入
 */
@Tag(name = "VIP会员")
@Slf4j
@RestController
@RequestMapping("/app/vip")
public class AppVipController {

    @Autowired
    private IVipOrderService vipOrderService;

    @Operation(summary = "购买VIP(99元/年,默认支付通过)")
    @PostMapping(value = "/purchase")
    public Result<VipPurchaseRespVO> purchase(@RequestBody VipPurchaseReqVO reqVO) {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error("用户信息错误！");
        }
        if (reqVO == null || ObjectUtil.isEmpty(reqVO.getPayChannel())) {
            return Result.error("请求参数错误");
        }
        try {
            VipPurchaseRespVO respVO = vipOrderService.purchase(
                    loginUser.getId(), reqVO.getPayChannel(), reqVO.getMock());
            return Result.OK(respVO);
        } catch (AppException e) {
            return Result.error(e.getMsg());
        }
    }

    @Operation(summary = "VIP信息(是否VIP/到期时间)")
    @GetMapping(value = "/info")
    public Result<VipInfoVO> info() {
        AppUser loginUser = AppAuthUtil.getUserInfo();
        if (ObjectUtil.isEmpty(loginUser)) {
            return Result.error("用户信息错误！");
        }
        try {
            return Result.OK(vipOrderService.getVipInfo(loginUser.getId()));
        } catch (AppException e) {
            return Result.error(e.getMsg());
        }
    }

}
