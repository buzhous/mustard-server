package org.jeecg.modules.app.openapi;

import org.jeecg.modules.app.service.IOpenAppCredentialService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 开放平台过滤器注册（order=11，位于 AppAuthFilter(order=10) 之后；
 * AppAuthFilter 已放行 /app/open/ 前缀，本过滤器为该前缀的唯一守门人）
 */
@Configuration
public class OpenFilterConfig {

    @Bean
    public FilterRegistrationBean<OpenSignatureFilter> openSignatureFilterRegistration(
            IOpenAppCredentialService credentialService,
            OpenRateLimiter rateLimiter,
            OpenAuditLogger auditLogger) {
        FilterRegistrationBean<OpenSignatureFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new OpenSignatureFilter(credentialService, rateLimiter, auditLogger));
        registration.addUrlPatterns("/app/open/*");
        registration.setOrder(11);
        registration.setName("openSignatureFilter");
        return registration;
    }
}
