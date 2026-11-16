package org.jeecg.modules.admin.controller.feedback;

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
import org.jeecg.modules.admin.entity.feedback.Feedback;
import org.jeecg.modules.admin.mapper.FeedbackMapper;
import org.jeecg.modules.admin.service.IFeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
 * 用户反馈 后台管理
 */
@Slf4j
@RestController
@RequestMapping("/feedback/admin")
@Tag(name = "用户反馈管理")
public class FeedbackAdminController extends JeecgController<Feedback, IFeedbackService> {

    @Autowired
    private IFeedbackService feedbackService;

    @Autowired
    private FeedbackMapper feedbackMapper;

    @Operation(summary = "反馈分页列表（支持 status 筛选与关键字查询）")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(Feedback feedback,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<Feedback> queryWrapper = QueryGenerator.initQueryWrapper(feedback, req.getParameterMap());
        queryWrapper.orderByDesc("create_time");
        Page<Feedback> page = new Page<>(pageNo, pageSize);
        IPage<Feedback> pageList = feedbackService.page(page, queryWrapper);
        fillUserInfo(pageList.getRecords());
        return Result.ok(pageList);
    }

    @Operation(summary = "反馈详情")
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id") String id) {
        Feedback record = feedbackService.getById(id);
        if (record == null) {
            return Result.error("未找到对应数据");
        }
        fillUserInfo(Collections.singletonList(record));
        return Result.ok(record);
    }

    @Operation(summary = "回复反馈（回复内容写入并标记已处理）")
    @PostMapping(value = "/reply")
    public Result<?> reply(@RequestBody Map<String, String> body) {
        String id = body == null ? null : body.get("id");
        String reply = body == null ? null : body.get("reply");
        if (!StringUtils.hasText(id)) {
            return Result.error("缺少反馈 id");
        }
        if (!StringUtils.hasText(reply)) {
            return Result.error("请填写回复内容");
        }
        if (reply.length() > 500) {
            return Result.error("回复内容不能超过 500 字");
        }
        Feedback exist = feedbackService.getById(id);
        if (exist == null) {
            return Result.error("未找到对应数据");
        }
        int rows = feedbackMapper.update(null, new LambdaUpdateWrapper<Feedback>()
                .eq(Feedback::getId, id)
                .set(Feedback::getReply, reply.trim())
                .set(Feedback::getStatus, Feedback.STATUS_RESOLVED)
                .set(Feedback::getUpdateTime, new Date()));
        return rows > 0 ? Result.ok("回复成功") : Result.error("回复失败");
    }

    @Operation(summary = "通过id删除反馈")
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        feedbackService.removeById(id);
        return Result.ok("删除成功!");
    }

    @Operation(summary = "批量删除反馈")
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids") String ids) {
        List<String> idList = splitIds(ids);
        if (idList.isEmpty()) {
            return Result.error("请选择要删除的数据");
        }
        feedbackService.removeByIds(idList);
        return Result.ok("批量删除成功!");
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

    /**
     * 列表补充提交用户昵称/手机号
     */
    private void fillUserInfo(List<Feedback> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<String> userIds = records.stream()
                .map(Feedback::getUserId)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }
        Map<String, Map<String, Object>> userMap = feedbackMapper.selectUserInfos(userIds).stream()
                .collect(Collectors.toMap(
                        m -> String.valueOf(m.get("id")),
                        m -> m,
                        (a, b) -> a));
        records.forEach(r -> {
            Map<String, Object> user = userMap.get(r.getUserId());
            if (user != null) {
                Object nickname = user.get("nickname");
                Object phone = user.get("phone");
                r.setNickname(nickname == null ? "" : String.valueOf(nickname));
                r.setPhone(phone == null ? "" : String.valueOf(phone));
            }
        });
    }
}
