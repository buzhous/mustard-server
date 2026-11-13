package org.jeecg.modules.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.admin.entity.topic.TopicAdmin;
import org.jeecg.modules.admin.mapper.TopicAdminMapper;
import org.jeecg.modules.admin.service.ITopicAdminService;
import org.jeecg.modules.admin.utils.TopicFieldClassifier;
import org.springframework.stereotype.Service;

/**
 * 添加物品主题管理 服务实现
 * <p>
 * 主题类型(topicType)在控制器层校验通过后由 {@link TopicFieldClassifier#classify(String)} 自动推导写入。
 */
@Service
public class TopicAdminServiceImpl extends ServiceImpl<TopicAdminMapper, TopicAdmin> implements ITopicAdminService {

    @Override
    public void saveTopic(TopicAdmin topic) {
        topic.setTopicType(TopicFieldClassifier.classify(topic.getFields()));
        save(topic);
    }

    @Override
    public void updateTopic(TopicAdmin topic) {
        topic.setTopicType(TopicFieldClassifier.classify(topic.getFields()));
        updateById(topic);
    }
}
