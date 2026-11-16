package org.jeecg.modules.admin.controller.coupon;

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
import org.jeecg.modules.admin.constant.CouponAdminConstant;
import org.jeecg.modules.admin.entity.coupon.CouponAdmin;
import org.jeecg.modules.admin.entity.coupon.UserCouponAdmin;
import org.jeecg.modules.admin.mapper.UserCouponAdminMapper;
import org.jeecg.modules.admin.service.ICouponAdminService;
import org.jeecg.modules.admin.service.IUserCouponAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
 * 用户持券 后台管理（发放/查询/作废）
 */
@Slf4j
@RestController
@RequestMapping("/coupon/admin/userCoupon")
@Tag(name = "用户持券管理")
public class UserCouponAdminController extends JeecgController<UserCouponAdmin, IUserCouponAdminService> {

    /** 单次发放张数上限 */
    private static final int GRANT_MAX_COUNT = 999;

    @Autowired
    private IUserCouponAdminService userCouponAdminService;

    @Autowired
    private UserCouponAdminMapper userCouponAdminMapper;

    @Autowired
    private ICouponAdminService couponAdminService;

    @Operation(summary = "持券分页列表查询（userKey按手机号/用户名/用户Id过滤，券名称模糊传 *关键字*）")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(UserCouponAdmin userCoupon,
                                   @RequestParam(name = "userKey", required = false) String userKey,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        if (StringUtils.hasText(userKey)) {
            String userId = userCouponAdminMapper.selectUserIdByKey(userKey.trim());
            if (userId == null) {
                return Result.error("未找到用户：" + userKey);
            }
            userCoupon.setUserId(userId);
        }
        QueryWrapper<UserCouponAdmin> queryWrapper = QueryGenerator.initQueryWrapper(userCoupon, req.getParameterMap());
        queryWrapper.orderByDesc("create_time");
        Page<UserCouponAdmin> page = new Page<>(pageNo, pageSize);
        IPage<UserCouponAdmin> pageList = userCouponAdminService.page(page, queryWrapper);
        fillUserName(pageList.getRecords());
        return Result.ok(pageList);
    }

    @Operation(summary = "发放优惠券给指定用户（按手机号/用户名/用户Id，系统发放，扣减券库存防超发）")
    @PostMapping(value = "/grant")
    public Result<?> grant(@RequestParam(name = "userKey") String userKey,
                           @RequestParam(name = "couponId") String couponId,
                           @RequestParam(name = "count", defaultValue = "1") Integer count) {
        if (!StringUtils.hasText(userKey)) {
            return Result.error("请填写要发放的用户（手机号/用户名/用户Id）");
        }
        if (!StringUtils.hasText(couponId)) {
            return Result.error("请选择要发放的优惠券");
        }
        if (count == null || count < 1) {
            return Result.error("发放张数必须大于0");
        }
        if (count > GRANT_MAX_COUNT) {
            return Result.error("单次发放不能超过 " + GRANT_MAX_COUNT + " 张");
        }
        String userId = userCouponAdminMapper.selectUserIdByKey(userKey.trim());
        if (userId == null) {
            return Result.error("未找到用户：" + userKey);
        }
        CouponAdmin coupon = couponAdminService.getById(couponId);
        if (coupon == null) {
            return Result.error("未找到对应优惠券");
        }
        if (!CouponAdminConstant.COUPON_STATUS_PUBLISHED.equals(coupon.getStatus())) {
            return Result.error("仅已发布的优惠券可发放（当前状态不可发放）");
        }
        if (coupon.getValidEndTime() != null && coupon.getValidEndTime().before(new Date())) {
            return Result.error("该券已过有效期，不可发放");
        }
        userCouponAdminService.grantCoupon(userId, couponId, count);
        return Result.ok("成功发放 " + count + " 张「" + coupon.getCouponName() + "」给用户：" + userKey.trim());
    }

    @Operation(summary = "批量作废持券（仅未使用状态可作废，作废后回补券库存）")
    @PutMapping(value = "/invalidate")
    public Result<?> invalidate(@RequestParam(name = "ids") String ids) {
        List<String> idList = splitIds(ids);
        if (idList.isEmpty()) {
            return Result.error("请选择要作废的持券记录");
        }
        int rows = userCouponAdminService.invalidateByIds(idList);
        int skipped = idList.size() - rows;
        String msg = "成功作废 " + rows + " 张（库存已回补）";
        if (skipped > 0) {
            msg += "，" + skipped + " 张跳过（仅未使用状态的券可作废）";
        }
        return Result.ok(msg);
    }

    private void fillUserName(List<UserCouponAdmin> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<String> userIds = records.stream()
                .map(UserCouponAdmin::getUserId)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }
        Map<String, String> nicknameMap = userCouponAdminMapper.selectUserNicknames(userIds).stream()
                .collect(Collectors.toMap(
                        m -> String.valueOf(m.get("id")),
                        m -> m.get("nickname") == null ? "" : String.valueOf(m.get("nickname")),
                        (a, b) -> a));
        records.forEach(r -> {
            if (StringUtils.hasText(r.getUserId())) {
                r.setUserName(nicknameMap.getOrDefault(r.getUserId(), ""));
            }
        });
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
