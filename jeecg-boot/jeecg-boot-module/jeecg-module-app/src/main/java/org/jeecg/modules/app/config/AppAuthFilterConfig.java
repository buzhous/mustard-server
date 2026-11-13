package org.jeecg.modules.app.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 显式注册 App 端鉴权过滤器（替代此前失效的 @WebFilter 注解注册）。
 * 匹配 /app/* 全部接口；放行名单在 AppAuthFilter.doFilter 内维护
 * （/app/login、/app/test、/app/open/**、非 /app/ 路径）。
 */
@Configuration
public class AppAuthFilterConfig {

    @Bean
    public FilterRegistrationBean<AppAuthFilter> appAuthFilterRegistration() {
        FilterRegistrationBean<AppAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AppAuthFilter());
        registration.addUrlPatterns("/app/*");
        registration.setOrder(10);
        registration.setName("appAuthFilter");
        return registration;
    }
}
