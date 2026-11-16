package org.jeecg.modules.admin.controller.ruins;

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
import org.jeecg.modules.admin.constant.RuinsAdminConstant;
import org.jeecg.modules.admin.entity.ruins.ItemRuinsAdmin;
import org.jeecg.modules.admin.mapper.ItemRuinsAdminMapper;
import org.jeecg.modules.admin.service.IItemRuinsAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
 * 殷墟物品 后台管理
 */
@Slf4j
@RestController
@RequestMapping("/ruins/admin")
@Tag(name = "殷墟物品管理")
public class ItemRuinsAdminController extends JeecgController<ItemRuinsAdmin, IItemRuinsAdminService> {

    @Autowired
    private IItemRuinsAdminService itemRuinsAdminService;

    @Autowired
    private ItemRuinsAdminMapper itemRuinsAdminMapper;

    @Operation(summary = "殷墟物品分页列表查询")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(ItemRuinsAdmin itemRuins,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<ItemRuinsAdmin> queryWrapper = QueryGenerator.initQueryWrapper(itemRuins, req.getParameterMap());
        Page<ItemRuinsAdmin> page = new Page<>(pageNo, pageSize);
        IPage<ItemRuinsAdmin> pageList = itemRuinsAdminService.page(page, queryWrapper);
        fillPickupUserName(pageList.getRecords());
        return Result.ok(pageList);
    }

    @Operation(summary = "殷墟物品详情")
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id") String id) {
        ItemRuinsAdmin ruins = itemRuinsAdminService.getById(id);
        if (ruins == null) {
            return Result.error("未找到对应数据");
        }
        fillPickupUserName(Collections.singletonList(ruins));
        return Result.ok(ruins);
    }

    @Operation(summary = "通过id删除殷墟物品")
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        itemRuinsAdminService.removeById(id);
        return Result.ok("删除成功!");
    }

    @Operation(summary = "批量删除殷墟物品")
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids") String ids) {
        List<String> idList = splitIds(ids);
        if (idList.isEmpty()) {
            return Result.error("请选择要删除的数据");
        }
        itemRuinsAdminService.removeByIds(idList);
        return Result.ok("批量删除成功!");
    }

    @Operation(summary = "批量下架（仅待拾取状态可下架，下架后App广场不展示且不可拾取）")
    @PutMapping(value = "/offShelfBatch")
    public Result<?> offShelfBatch(@RequestParam(name = "ids") String ids) {
        List<String> idList = splitIds(ids);
        if (idList.isEmpty()) {
            return Result.error("请选择要下架的数据");
        }
        // 仅待拾取(status=0)的记录可下架
        int rows = itemRuinsAdminMapper.update(null, new LambdaUpdateWrapper<ItemRuinsAdmin>()
                .in(ItemRuinsAdmin::getId, idList)
                .eq(ItemRuinsAdmin::getStatus, RuinsAdminConstant.STATUS_NOT_PICKUP)
                .set(ItemRuinsAdmin::getStatus, RuinsAdminConstant.STATUS_OFF_SHELF)
                .set(ItemRuinsAdmin::getUpdateTime, new Date()));
        int skipped = idList.size() - rows;
        String msg = "成功下架 " + rows + " 件";
        if (skipped > 0) {
            msg += "，" + skipped + " 件跳过（仅待拾取状态的物品可下架）";
        }
        return Result.ok(msg);
    }

    @Operation(summary = "批量指定用户拾取（按手机号/用户名/用户Id指派，仅待拾取状态可指派，被指定用户外不可见）")
    @PutMapping(value = "/assignPickupBatch")
    public Result<?> assignPickupBatch(@RequestParam(name = "ids") String ids,
                                       @RequestParam(name = "userKey") String userKey) {
        List<String> idList = splitIds(ids);
        if (idList.isEmpty()) {
            return Result.error("请选择要指派的数据");
        }
        if (!StringUtils.hasText(userKey)) {
            return Result.error("请填写要指派的用户（手机号/用户名/用户Id）");
        }
        String targetUserId = itemRuinsAdminMapper.selectUserIdByKey(userKey.trim());
        if (targetUserId == null) {
            return Result.error("未找到用户：" + userKey);
        }

        int rows = itemRuinsAdminMapper.update(null, new LambdaUpdateWrapper<ItemRuinsAdmin>()
                .in(ItemRuinsAdmin::getId, idList)
                .eq(ItemRuinsAdmin::getStatus, RuinsAdminConstant.STATUS_NOT_PICKUP)
                .set(ItemRuinsAdmin::getPickupUser, targetUserId)
                .set(ItemRuinsAdmin::getUpdateTime, new Date()));
        int skipped = idList.size() - rows;
        String msg = "成功指派 " + rows + " 件给用户：" + targetUserId;
        if (skipped > 0) {
            msg += "，" + skipped + " 件跳过（仅待拾取状态的物品可指派）";
        }
        return Result.ok(msg);
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

    private void fillPickupUserName(List<ItemRuinsAdmin> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<String> pickupUserIds = records.stream()
                .map(ItemRuinsAdmin::getPickupUser)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (pickupUserIds.isEmpty()) {
            return;
        }
        Map<String, String> nicknameMap = itemRuinsAdminMapper.selectUserNicknames(pickupUserIds).stream()
                .collect(Collectors.toMap(
                        m -> String.valueOf(m.get("id")),
                        m -> m.get("nickname") == null ? "" : String.valueOf(m.get("nickname")),
                        (a, b) -> a));
        records.forEach(r -> {
            if (StringUtils.hasText(r.getPickupUser())) {
                r.setPickupUserName(nicknameMap.getOrDefault(r.getPickupUser(), ""));
            }
        });
    }
}
