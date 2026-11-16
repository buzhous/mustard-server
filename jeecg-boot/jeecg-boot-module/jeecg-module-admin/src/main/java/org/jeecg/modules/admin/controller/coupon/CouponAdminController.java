package org.jeecg.modules.admin.controller.coupon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.jeecg.modules.admin.mapper.CouponAdminMapper;
import org.jeecg.modules.admin.mapper.UserCouponAdminMapper;
import org.jeecg.modules.admin.service.ICouponAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 优惠券模板 后台管理（平台级券统一管理入口）
 */
@Slf4j
@RestController
@RequestMapping("/coupon/admin")
@Tag(name = "优惠券模板管理")
public class CouponAdminController extends JeecgController<CouponAdmin, ICouponAdminService> {

    @Autowired
    private ICouponAdminService couponAdminService;

    @Autowired
    private CouponAdminMapper couponAdminMapper;

    @Autowired
    private UserCouponAdminMapper userCouponAdminMapper;

    @Operation(summary = "优惠券分页列表查询（名称模糊传 *关键字*）")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(CouponAdmin coupon,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<CouponAdmin> queryWrapper = QueryGenerator.initQueryWrapper(coupon, req.getParameterMap());
        queryWrapper.orderByAsc("sort").orderByDesc("create_time");
        Page<CouponAdmin> page = new Page<>(pageNo, pageSize);
        IPage<CouponAdmin> pageList = couponAdminService.page(page, queryWrapper);
        return Result.ok(pageList);
    }

    @Operation(summary = "优惠券详情")
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id") String id) {
        CouponAdmin coupon = couponAdminService.getById(id);
        if (coupon == null) {
            return Result.error("未找到对应数据");
        }
        return Result.ok(coupon);
    }

    @Operation(summary = "新增优惠券（默认未发布状态，发布请使用发布操作）")
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody CouponAdmin coupon) {
        String error = validateCoupon(coupon);
        if (error != null) {
            return Result.error(error);
        }
        if (couponAdminService.count(new LambdaQueryWrapper<CouponAdmin>().eq(CouponAdmin::getCode, coupon.getCode().trim())) > 0) {
            return Result.error("券编码已存在：" + coupon.getCode());
        }
        Date now = new Date();
        coupon.setId(null);
        coupon.setCode(coupon.getCode().trim());
        if (coupon.getDiscountValue() == null) {
            coupon.setDiscountValue(BigDecimal.ZERO);
        }
        if (coupon.getMinAmount() == null) {
            coupon.setMinAmount(BigDecimal.ZERO);
        }
        coupon.setRemainCount(coupon.getTotalCount());
        coupon.setUsedCount(0);
        if (coupon.getPerLimit() == null) {
            coupon.setPerLimit(0);
        }
        if (coupon.getDailyLimit() == null) {
            coupon.setDailyLimit(0);
        }
        coupon.setStatus(CouponAdminConstant.COUPON_STATUS_UNPUBLISHED);
        if (coupon.getSort() == null) {
            coupon.setSort(0);
        }
        coupon.setCreateTime(now);
        coupon.setUpdateTime(now);
        couponAdminService.save(coupon);
        return Result.ok("添加成功！券为未发布状态，请发布后生效");
    }

    @Operation(summary = "编辑优惠券（剩余数量/已使用数量/状态不可直接修改，库存请使用库存调整）")
    @PutMapping(value = "/edit")
    public Result<?> edit(@RequestBody CouponAdmin coupon) {
        if (!StringUtils.hasText(coupon.getId())) {
            return Result.error("缺少主键id");
        }
        CouponAdmin exist = couponAdminService.getById(coupon.getId());
        if (exist == null) {
            return Result.error("未找到对应数据");
        }
        String error = validateCoupon(coupon);
        if (error != null) {
            return Result.error(error);
        }
        String newCode = coupon.getCode().trim();
        if (!newCode.equals(exist.getCode())
                && couponAdminService.count(new LambdaQueryWrapper<CouponAdmin>().eq(CouponAdmin::getCode, newCode)) > 0) {
            return Result.error("券编码已存在：" + newCode);
        }
        coupon.setCode(newCode);
        // 剩余数量/已使用数量/状态/总库存 不可直接修改（库存调整走 adjustStock）
        coupon.setRemainCount(null);
        coupon.setUsedCount(null);
        coupon.setStatus(null);
        coupon.setTotalCount(null);
        coupon.setUpdateTime(new Date());
        couponAdminService.updateById(coupon);
        return Result.ok("更新成功!");
    }

    @Operation(summary = "通过id删除优惠券（已有用户持券记录的券禁止删除）")
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        return deleteWithValidate(Collections.singletonList(id));
    }

    @Operation(summary = "批量删除优惠券（已有用户持券记录的券禁止删除）")
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids") String ids) {
        List<String> idList = splitIds(ids);
        if (idList.isEmpty()) {
            return Result.error("请选择要删除的数据");
        }
        return deleteWithValidate(idList);
    }

    @Operation(summary = "批量发布（未发布/已下架 → 已发布）")
    @PutMapping(value = "/publishBatch")
    public Result<?> publishBatch(@RequestParam(name = "ids") String ids) {
        List<String> idList = splitIds(ids);
        if (idList.isEmpty()) {
            return Result.error("请选择要发布的数据");
        }
        int rows = couponAdminMapper.update(null, new LambdaUpdateWrapper<CouponAdmin>()
                .in(CouponAdmin::getId, idList)
                .in(CouponAdmin::getStatus, CouponAdminConstant.COUPON_STATUS_UNPUBLISHED, CouponAdminConstant.COUPON_STATUS_OFFLINE)
                .set(CouponAdmin::getStatus, CouponAdminConstant.COUPON_STATUS_PUBLISHED)
                .set(CouponAdmin::getUpdateTime, new Date()));
        int skipped = idList.size() - rows;
        String msg = "成功发布 " + rows + " 张";
        if (skipped > 0) {
            msg += "，" + skipped + " 张跳过（仅未发布/已下架状态的券可发布）";
        }
        return Result.ok(msg);
    }

    @Operation(summary = "批量下架（已发布 → 已下架，下架后App不可领取）")
    @PutMapping(value = "/offlineBatch")
    public Result<?> offlineBatch(@RequestParam(name = "ids") String ids) {
        List<String> idList = splitIds(ids);
        if (idList.isEmpty()) {
            return Result.error("请选择要下架的数据");
        }
        int rows = couponAdminMapper.update(null, new LambdaUpdateWrapper<CouponAdmin>()
                .in(CouponAdmin::getId, idList)
                .eq(CouponAdmin::getStatus, CouponAdminConstant.COUPON_STATUS_PUBLISHED)
                .set(CouponAdmin::getStatus, CouponAdminConstant.COUPON_STATUS_OFFLINE)
                .set(CouponAdmin::getUpdateTime, new Date()));
        int skipped = idList.size() - rows;
        String msg = "成功下架 " + rows + " 张";
        if (skipped > 0) {
            msg += "，" + skipped + " 张跳过（仅已发布状态的券可下架）";
        }
        return Result.ok(msg);
    }

    @Operation(summary = "库存调整（正数增加库存、负数减少库存，总量与剩余同步调整）")
    @PutMapping(value = "/adjustStock")
    public Result<?> adjustStock(@RequestParam(name = "id") String id,
                                 @RequestParam(name = "delta") Integer delta) {
        if (delta == null || delta == 0) {
            return Result.error("调整数量不能为0");
        }
        CouponAdmin coupon = couponAdminService.getById(id);
        if (coupon == null) {
            return Result.error("未找到对应数据");
        }
        int rows = couponAdminMapper.adjustStock(id, delta);
        if (rows == 0) {
            return Result.error("调整失败：减少后的剩余库存不能为负数");
        }
        String action = delta > 0 ? "增加" : "减少";
        return Result.ok("已" + action + "库存 " + Math.abs(delta) + " 张（当前剩余 " + (coupon.getRemainCount() + delta) + "）");
    }

    /**
     * 删除（含批量）：已有用户持券记录的券禁止删除
     */
    private Result<?> deleteWithValidate(List<String> idList) {
        List<CouponAdmin> coupons = couponAdminService.listByIds(idList);
        if (coupons.isEmpty()) {
            return Result.error("未找到对应数据");
        }
        List<String> couponIds = coupons.stream().map(CouponAdmin::getId).collect(Collectors.toList());
        List<UserCouponAdmin> userCoupons = userCouponAdminMapper.selectList(new LambdaQueryWrapper<UserCouponAdmin>()
                .in(UserCouponAdmin::getCouponId, couponIds)
                .select(UserCouponAdmin::getCouponId));
        if (!userCoupons.isEmpty()) {
            Map<String, Long> couponIdSet = userCoupons.stream()
                    .collect(Collectors.groupingBy(UserCouponAdmin::getCouponId, Collectors.counting()));
            String names = coupons.stream()
                    .filter(c -> couponIdSet.containsKey(c.getId()))
                    .map(CouponAdmin::getCouponName)
                    .collect(Collectors.joining("、"));
            return Result.error("以下券已有用户持券记录，禁止删除：" + names + "（可改用下架）");
        }
        couponAdminService.removeByIds(couponIds);
        return Result.ok("删除成功!");
    }

    /**
     * 新增/编辑公共校验
     */
    private String validateCoupon(CouponAdmin coupon) {
        if (!StringUtils.hasText(coupon.getCode())) {
            return "券编码不能为空";
        }
        if (!StringUtils.hasText(coupon.getCouponName())) {
            return "券名称不能为空";
        }
        Integer type = coupon.getCouponType();
        if (type == null || type < CouponAdminConstant.TYPE_FULL_REDUCTION || type > CouponAdminConstant.TYPE_EXCHANGE) {
            return "券类型不合法（1满减/2折扣/3兑换）";
        }
        if (type != CouponAdminConstant.TYPE_EXCHANGE
                && (coupon.getDiscountValue() == null || coupon.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0)) {
            return "满减/折扣券必须填写面额（大于0）";
        }
        if (type == CouponAdminConstant.TYPE_DISCOUNT
                && (coupon.getDiscountValue() == null || coupon.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0
                || coupon.getDiscountValue().compareTo(BigDecimal.TEN) >= 0)) {
            return "折扣券折扣率应为大于0小于10的数值（如8.5折填8.5）";
        }
        if (coupon.getValidStartTime() == null || coupon.getValidEndTime() == null) {
            return "有效期开始/结束时间不能为空";
        }
        if (!coupon.getValidEndTime().after(coupon.getValidStartTime())) {
            return "有效期结束时间必须晚于开始时间";
        }
        if (coupon.getTotalCount() == null || coupon.getTotalCount() < 0) {
            return "总库存必须大于等于0";
        }
        return null;
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
