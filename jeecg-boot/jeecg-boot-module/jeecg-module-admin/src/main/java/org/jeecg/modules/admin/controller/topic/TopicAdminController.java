package org.jeecg.modules.admin.controller.topic;

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
import org.jeecg.modules.admin.entity.topic.TopicAdmin;
import org.jeecg.modules.admin.service.ITopicAdminService;
import org.jeecg.modules.admin.utils.MipProfileSupport;
import org.jeecg.modules.admin.utils.TopicFieldClassifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Date;

/**
 * 添加物品主题 后台管理
 */
@Slf4j
@RestController
@RequestMapping("/topic/admin")
@Tag(name = "添加物品主题管理")
public class TopicAdminController extends JeecgController<TopicAdmin, ITopicAdminService> {

    @Autowired
    private ITopicAdminService topicAdminService;

    @Operation(summary = "主题分页列表查询")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(TopicAdmin topic,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<TopicAdmin> queryWrapper = QueryGenerator.initQueryWrapper(topic, req.getParameterMap());
        Page<TopicAdmin> page = new Page<>(pageNo, pageSize);
        IPage<TopicAdmin> pageList = topicAdminService.page(page, queryWrapper);
        return Result.ok(pageList);
    }

    @Operation(summary = "主题详情")
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id") String id) {
        TopicAdmin topic = topicAdminService.getById(id);
        if (topic == null) {
            return Result.error("未找到对应数据");
        }
        return Result.ok(topic);
    }

    @Operation(summary = "新增主题（字段配置必须包含名称/图标/类别，主题类型自动推导）")
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody TopicAdmin topic) {
        if (topic.getTopicName() == null || topic.getTopicName().isBlank()) {
            return Result.error("主题名称不能为空");
        }
        String fieldError = TopicFieldClassifier.validate(topic.getFields());
        if (fieldError != null) {
            return Result.error(fieldError);
        }
        String mipError = MipProfileSupport.validateFields(topic.getFields());
        if (mipError != null) {
            return Result.error(mipError);
        }
        Date now = new Date();
        topic.setId(null);
        topic.setCreateTime(now);
        topic.setUpdateTime(now);
        if (topic.getTopicStatus() == null) {
            topic.setTopicStatus(0);
        }
        topicAdminService.saveTopic(topic);
        return Result.ok("添加成功！当前主题类型：" + typeLabel(topic.getTopicType()));
    }

    @Operation(summary = "编辑主题（字段配置必须包含名称/图标/类别，主题类型自动推导）")
    @PutMapping(value = "/edit")
    public Result<?> edit(@RequestBody TopicAdmin topic) {
        if (topic.getId() == null) {
            return Result.error("缺少主键id");
        }
        TopicAdmin exist = topicAdminService.getById(topic.getId());
        if (exist == null) {
            return Result.error("未找到对应数据");
        }
        if (topic.getTopicName() == null || topic.getTopicName().isBlank()) {
            return Result.error("主题名称不能为空");
        }
        String fieldError = TopicFieldClassifier.validate(topic.getFields());
        if (fieldError != null) {
            return Result.error(fieldError);
        }
        String mipError = MipProfileSupport.validateFields(topic.getFields());
        if (mipError != null) {
            return Result.error(mipError);
        }
        topic.setUpdateTime(new Date());
        topicAdminService.updateTopic(topic);
        return Result.ok("更新成功！当前主题类型：" + typeLabel(topic.getTopicType()));
    }

    @Operation(summary = "通过id删除主题")
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        topicAdminService.removeById(id);
        return Result.ok("删除成功!");
    }

    @Operation(summary = "批量删除主题")
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids") String ids) {
        topicAdminService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.ok("批量删除成功!");
    }

    private String typeLabel(Integer topicType) {
        if (topicType == null) {
            return "未知";
        }
        return switch (topicType) {
            case TopicFieldClassifier.TYPE_MODEL -> "模型型";
            case TopicFieldClassifier.TYPE_MEDIA -> "多媒体型";
            case TopicFieldClassifier.TYPE_EXTENDED -> "扩展型";
            case TopicFieldClassifier.TYPE_BASIC -> "基础型";
            default -> "未知";
        };
    }
}
