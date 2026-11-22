package org.jeecg.modules.app.openapi;

import java.util.Map;
import java.util.Set;

/**
 * 开放平台 scope 权限分级校验（设计方案 3.2 / E2）
 * <p>
 * - REST 出口：按"路径 + 方法"精确表判定（Filter 层调用）；
 * - MCP 出口：JSON-RPC 的 tools/call 无法在路径层区分读写意图，按 tool 名判定（dispatch 层调用）；
 * - 不在表中的路径视为只读（新未上线接口自然落到 controller 404，scope 检查先于 404，
 *   保证验收矩阵 S-2「readonly 调 add → 403 先于 404」成立）。
 */
public final class OpenScopeChecker {

    private OpenScopeChecker() {
    }

    public static final String SCOPE_READONLY = "readonly";
    public static final String SCOPE_READWRITE = "readwrite";

    /** REST 写接口精确表（路径 → 所需 scope），均为 POST */
    private static final Map<String, String> REST_WRITE_PATHS = Map.of(
            "/app/open/item/add", SCOPE_READWRITE,
            "/app/open/item/update", SCOPE_READWRITE,
            "/app/open/item/delete", SCOPE_READWRITE,
            "/app/open/gift/preview", SCOPE_READWRITE,
            "/app/open/gift/send", SCOPE_READWRITE
    );

    /** MCP 写工具清单（readwrite 才可调用） */
    public static final Set<String> MCP_WRITE_TOOLS = Set.of("add_item", "update_item", "delete_item", "gift_item");

    /**
     * 该请求是否为写操作（REST：POST 且命中写路径精确表）
     */
    public static boolean isWriteRequest(String path, String method) {
        return "POST".equalsIgnoreCase(method) && REST_WRITE_PATHS.containsKey(path);
    }

    /**
     * scope 校验：命中写路径且凭据非 readwrite 时拒绝
     *
     * @return null=通过；否则返回拒绝原因（用于 403 message）
     */
    public static String checkRest(String path, String method, String credentialScope) {
        if (!isWriteRequest(path, method)) {
            return null;
        }
        if (SCOPE_READWRITE.equals(credentialScope)) {
            return null;
        }
        return "scope 不足：" + REST_WRITE_PATHS.get(path) + " 权限才能调用写接口";
    }

    /**
     * MCP 工具 scope 校验（dispatch 层按 tool 名判定）
     *
     * @return null=通过；否则返回拒绝原因
     */
    public static String checkTool(String toolName, String credentialScope) {
        if (!MCP_WRITE_TOOLS.contains(toolName)) {
            return null;
        }
        if (SCOPE_READWRITE.equals(credentialScope)) {
            return null;
        }
        return "scope 不足：" + toolName + " 需要 readwrite 权限";
    }
}
