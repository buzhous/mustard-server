package org.jeecg.modules.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.admin.entity.feedback.Feedback;
import org.jeecg.modules.admin.mapper.FeedbackMapper;
import org.jeecg.modules.admin.service.IFeedbackService;
import org.springframework.stereotype.Service;

/**
 * 用户反馈 服务实现
 */
@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback>
        implements IFeedbackService {
}
