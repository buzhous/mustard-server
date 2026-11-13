package org.jeecg.modules.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.admin.entity.topic.TopicAdmin;

public interface ITopicAdminService extends IService<TopicAdmin> {

    /**
     * 新增主题：校验字段配置必填三要素，自动推导主题类型
     */
    void saveTopic(TopicAdmin topic);

    /**
     * 更新主题：校验字段配置必填三要素，自动推导主题类型
     */
    void updateTopic(TopicAdmin topic);
}
