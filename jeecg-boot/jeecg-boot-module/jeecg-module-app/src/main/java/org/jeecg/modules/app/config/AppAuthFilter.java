package org.jeecg.modules.app.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import com.alibaba.fastjson.JSON;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.constant.AuthConstant;
import org.jeecg.modules.app.utils.SecurityTokenUtil;

import java.io.IOException;

/**
 * App 端鉴权过滤器：
 * 校验请求头 x-access-token（App JWT），解析出 uid 并注入 x-uid 供业务读取（AppAuthUtil）。
 *
 * 注意：必须通过 FilterRegistrationBean 显式注册（见 AppAuthFilterConfig），
 * 此前依赖 @WebFilter 注解，但启动类未开启 @ServletComponentScan，过滤器从未生效。
 */
public class AppAuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化操作
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        CustomRequestWrapper requestWrapper = new CustomRequestWrapper(httpRequest);

        // 获取请求URL
        String url = httpRequest.getRequestURL().toString();

        // 登陆跳过、开放平台接口跳过（开放接口独立 AK/SK 验签）、文章（协议需登录前可读）跳过、非app项目跳过
        if (url.contains("/app/login") || url.contains("/app/open/") || url.contains("/app/article") || !url.contains("/app/")) {
            chain.doFilter(request, response);
            return;
        }
        if (url.contains("/app/test")) {
            chain.doFilter(request, response);
            return;
        }
        // 获取请求头中的参数
        String accessToken = httpRequest.getHeader(AuthConstant.OAUTH_TOKEN_HEADER);
        try {
            if (StrUtil.isEmpty(accessToken) || accessToken.split("\\.").length != 3) {
                // 验签失败，返回错误响应
                this.responseJson(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Token", response);
                return;
            }
            var jwt = SecurityTokenUtil.parseJwtToken(accessToken);
            if (jwt == null || StrUtil.isEmpty(jwt.getStr(AuthConstant.JWT_UID_HEADER))) {
                // 验签失败，返回错误响应
                this.responseJson(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Auth", response);
                return;
            }

            String uid = jwt.getStr(AuthConstant.JWT_UID_HEADER);
            requestWrapper.addHeader(AuthConstant.USER_ID_HEADER, uid);
            chain.doFilter(requestWrapper, response);

        } catch (IOException e) {
            this.responseJson(HttpServletResponse.SC_BAD_REQUEST, "System Error", response);
        }
    }

    @Override
    public void destroy() {
        // ... 销毁操作
    }

    private void responseJson(int code, String msg, ServletResponse response) throws IOException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setContentType(ContentType.JSON.getValue());
        httpResponse.setStatus(code);
        httpResponse.getWriter().write(JSON.toJSONString(Result.error(code, msg)));
    }
}
