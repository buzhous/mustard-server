package org.jeecg.modules.app.openapi;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.entity.OpenAppCredential;
import org.jeecg.modules.app.service.IOpenAppCredentialService;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * 开放平台统一入口过滤器（设计方案 3.1/3.2/3.3/3.4，上移自 AppOpenApiController.checkAppKey）
 * <p>
 * 匹配 /app/open/**（AppAuthFilter 已放行该前缀，JWT 与开放凭据互不干扰），行为与既有 checkAppKey 一致（零改造）：
 * appkey 请求头 → 启用凭据 → IP 白名单；叠加 scope 分级（REST 写路径）与限流配额，审计异步落库。
 * <p>
 * 通过后凭据放 request attribute {@link #ATTR_CREDENTIAL}，出口层直接取用（一次查询复用）。
 * MCP 出口（/app/open/mcp/**）的写工具 scope 在 dispatch 层按 tool 名判定（路径层无法区分 tools/call 读写意图）。
 */
@Slf4j
public class OpenSignatureFilter implements Filter {

    /** request attribute：通过鉴权的凭据对象 */
    public static final String ATTR_CREDENTIAL = "OPEN_CREDENTIAL";

    /** MCP 出口前缀（scope 在 dispatch 层按 tool 判定） */
    private static final String MCP_PREFIX = "/app/open/mcp";

    private final IOpenAppCredentialService credentialService;
    private final OpenRateLimiter rateLimiter;
    private final OpenAuditLogger auditLogger;

    public OpenSignatureFilter(IOpenAppCredentialService credentialService,
                               OpenRateLimiter rateLimiter,
                               OpenAuditLogger auditLogger) {
        this.credentialService = credentialService;
        this.rateLimiter = rateLimiter;
        this.auditLogger = auditLogger;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();
        String clientIp = resolveClientIp(request);
        long start = System.currentTimeMillis();

        OpenAppCredential credential = null;
        int resultCode = 200;
        boolean write = false;
        try {
            // ① 鉴权：appkey 请求头 → 启用凭据（行为与既有 checkAppKey 一致）
            String appKey = request.getHeader("appkey");
            if (StrUtil.isEmpty(appKey)) {
                resultCode = reject(response, path, method, clientIp, 401, "缺少 appkey 请求头");
                return;
            }
            credential = credentialService.getEnabledByAppKey(appKey);
            if (ObjectUtil.isEmpty(credential)) {
                resultCode = reject(response, path, method, clientIp, 401, "无效的 appkey 或凭据已停用");
                return;
            }
            if (StrUtil.isEmpty(credential.getBoundUid())) {
                resultCode = reject(response, path, method, clientIp, 401, "凭据未绑定数据范围用户，请联系平台管理员");
                return;
            }
            // IP 白名单
            String whiteList = credential.getWhiteList();
            if (StrUtil.isNotBlank(whiteList)) {
                boolean matched = StrUtil.split(whiteList, ',').stream()
                        .anyMatch(ip -> ip.trim().equals(clientIp));
                if (!matched) {
                    resultCode = reject(response, path, method, clientIp, 403, "IP 未授权：" + clientIp);
                    return;
                }
            }
            // ② scope 分级（仅 REST 出口；MCP 在 dispatch 层按 tool 判定）
            boolean mcp = path.startsWith(MCP_PREFIX);
            write = OpenScopeChecker.isWriteRequest(path, method) && !mcp;
            if (!mcp) {
                String scopeError = OpenScopeChecker.checkRest(path, method, credential.getScope());
                if (scopeError != null) {
                    resultCode = reject(response, path, method, clientIp, 403, scopeError);
                    return;
                }
            }
            // ③ 限流配额（固定窗口；MCP 写工具在 dispatch 层追加写窗口）
            String limitError = rateLimiter.tryAcquire(appKey, write, credential.getDailyQuota());
            if (limitError != null) {
                response.setHeader("X-RateLimit-Reset", String.valueOf(rateLimiter.minuteResetEpochSecond()));
                resultCode = reject(response, path, method, clientIp, 429, limitError);
                return;
            }
            // ④ 凭据透传给出口层（一次查询复用）
            request.setAttribute(ATTR_CREDENTIAL, credential);
            chain.doFilter(servletRequest, servletResponse);
            resultCode = response.getStatus();
        } finally {
            long costMs = System.currentTimeMillis() - start;
            boolean mcpPath = path.startsWith(MCP_PREFIX);
            auditLogger.log(credential == null ? StrUtil.nullToEmpty(request.getHeader("appkey")) : credential.getAppKey(),
                    credential == null ? "" : credential.getBoundUid(),
                    mcpPath ? "mcp" : "rest", path, write ? "write" : "read",
                    StrUtil.nullToEmpty(request.getQueryString()),
                    resultCode, null, (int) costMs, clientIp);
        }
    }

    /**
     * 拒绝响应：统一 Result envelope，HTTP 状态码与 code 同步（设计方案 3.6）
     *
     * @return code（供审计记录）
     */
    private int reject(HttpServletResponse response, String path, String method, String clientIp,
                       int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(JSON.toJSONString(Result.error(code, message)));
        }
        log.info("[open-filter] {} {} -> {} {}", method, path, code, message);
        return code;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        if (StrUtil.isBlank(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
