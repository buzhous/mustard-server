package org.jeecg.modules.app.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 设置静态参数初始化
 */
@Lazy(false)
@Component
@Data
public class SmsStaticConfig {

    @Value("${jeecg.sms.accessKey:}")
    private String accessKeyId;

    @Value("${jeecg.sms.secretKey:}")
    private String accessKeySecret;

}
