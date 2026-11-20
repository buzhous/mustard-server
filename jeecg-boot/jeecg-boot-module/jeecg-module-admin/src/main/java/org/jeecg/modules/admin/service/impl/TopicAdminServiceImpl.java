package org.jeecg.modules.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.admin.entity.topic.TopicAdmin;
import org.jeecg.modules.admin.mapper.TopicAdminMapper;
import org.jeecg.modules.admin.service.ITopicAdminService;
import org.jeecg.modules.admin.utils.MipProfileSupport;
import org.jeecg.modules.admin.utils.TopicFieldClassifier;
import org.springframework.stereotype.Service;

/**
 * 添加物品主题管理 服务实现
 * <p>
 * 主题类型(topicType)在控制器层校验通过后由 {@link TopicFieldClassifier#classify(String)} 自动推导写入。
 * MIP v1：保存/更新时由服务端自动生成 mip_profile（协议 3.3 生成规则，P1/T4 钩子），
 * 模板字段变更即整体升级 Profile，实例侧由迁移接口全量重刷（整体升级策略，协议第 9 章）。
 */
@Service
public class TopicAdminServiceImpl extends ServiceImpl<TopicAdminMapper, TopicAdmin> implements ITopicAdminService {

    @Override
    public void saveTopic(TopicAdmin topic) {
        topic.setTopicType(TopicFieldClassifier.classify(topic.getFields()));
        save(topic);
        // mip_profile 含 schema_id=topic_{id}，主键生成后落库
        topic.setMipProfile(MipProfileSupport.buildProfileJson(topic.getFields(), "topic_" + topic.getId()));
        updateById(topic);
    }

    @Override
    public void updateTopic(TopicAdmin topic) {
        topic.setTopicType(TopicFieldClassifier.classify(topic.getFields()));
        topic.setMipProfile(MipProfileSupport.buildProfileJson(topic.getFields(), "topic_" + topic.getId()));
        updateById(topic);
    }
}
