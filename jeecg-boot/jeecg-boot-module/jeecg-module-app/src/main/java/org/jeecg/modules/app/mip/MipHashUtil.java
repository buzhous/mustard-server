package org.jeecg.modules.app.mip;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * MIP v1 哈希工具（MIP 协议第 6 章 / 5.3）
 * <p>
 * 规范化序列化：key 字典序递归排序、无空白字符、UTF-8 字节。
 * - content_hash = sha256(canonical(identity + attributes + custody))（L2 段）
 * - 账本 hash    = sha256(prev_hash + canonical(事件体))
 */
public final class MipHashUtil {

    /** 链首事件的 prev_hash */
    public static final String GENESIS_PREV_HASH = repeat('0', 64);

    private MipHashUtil() {
    }

    /**
     * 规范化：JSONObject 递归按 key 字典序排序；JSONArray 保持顺序；无空白；UTF-8
     */
    public static String canonical(Object o) {
        if (o instanceof JSONObject j) {
            TreeMap<String, Object> t = new TreeMap<>();
            for (Map.Entry<String, Object> e : j.entrySet()) {
                t.put(e.getKey(), e.getValue());
            }
            return t.entrySet().stream()
                    .map(e -> quote(e.getKey()) + ":" + canonical(e.getValue()))
                    .collect(Collectors.joining(",", "{", "}"));
        }
        if (o instanceof JSONArray a) {
            return a.stream().map(MipHashUtil::canonical)
                    .collect(Collectors.joining(",", "[", "]"));
        }
        if (o == null) {
            return "null";
        }
        if (o instanceof String s) {
            return quote(s);
        }
        return String.valueOf(o);
    }

    /**
     * L2 段（identity+attributes+custody）的规范化哈希
     */
    public static String contentHash(JSONObject instance) {
        JSONObject l2 = new JSONObject();
        l2.put("identity", instance.get("identity"));
        l2.put("attributes", instance.get("attributes"));
        l2.put("custody", instance.get("custody"));
        return sha256Hex(canonical(l2));
    }

    /**
     * 账本事件哈希：sha256(prev_hash + canonical(事件体))
     */
    public static String eventHash(String prevHash, Object eventBody) {
        return sha256Hex(prevHash + canonical(eventBody));
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // JVM 必备算法，不会发生
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
