package org.jeecg.modules.app.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.json.JSONObject;
import com.alibaba.fastjson.JSON;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.constant.AuthConstant;
import org.jeecg.modules.app.utils.SecurityTokenUtil;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

// Configuration 无法扫描到过滤包，urlPatterns不生效
@Configuration
@WebFilter(filterName = "AppAuthFilter", urlPatterns = {"/api/app/*"})
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
        System.out.println("url: " + url);

        // 登陆跳过、非app项目跳过
        if (url.contains("/app/login") || !url.contains("/app/")) {
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
            System.out.println("accessToken: " + accessToken);
            if (StrUtil.isEmpty(accessToken) || accessToken.split("\\.").length != 3) {
                // 验签失败，返回错误响应
                this.responseJson(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Token", response);
                return;
            }
            JSONObject jwt = SecurityTokenUtil.parseJwtToken(accessToken);
            if (jwt == null || StrUtil.isEmpty(jwt.getStr(AuthConstant.JWT_UID_HEADER))) {
                // 验签失败，返回错误响应
                this.responseJson(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Auth", response);
                return;
            }

            String uid = jwt.getStr(AuthConstant.JWT_UID_HEADER);
            requestWrapper.addHeader(AuthConstant.USER_ID_HEADER, uid);
            chain.doFilter(requestWrapper, response);

        } catch (IOException e) {
            e.printStackTrace();
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
