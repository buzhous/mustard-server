package org.jeecg.modules.app.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import org.jeecg.modules.app.constant.AuthConstant;

import java.util.Map;

public class SecurityTokenUtil {

    /**
     * 要解析 jwt 令牌的签名秘钥
     */
    static String JWT_SECRET = "ea2becdf7a5c5c381c1afb1535c168135c0a7f19";
    /**
     * 过期时间，默认12个月
     */
    static Long EXPIRE_TIME = 1000L * 60 * 60 * 24 * 30 * 12;
    /**
     * 过期时间，单位秒，默认12个月
     */
    public static Long EXPIRE_TIME_SECOND = 60 * 60 * 24 * 30 * 12L;

    /**
     * Token 续期宽限期：过期 30 天内仍可通过 refreshToken 静默续期，超过则拒绝
     */
    public static Long REFRESH_GRACE_MS = 1000L * 60 * 60 * 24 * 30L;

    public static Long expireTime() {
        return System.currentTimeMillis() + EXPIRE_TIME;
    }

    public static Long expireTime(Long time) {
        return time + EXPIRE_TIME;
    }

    public static String createJwtToken(Map<String, Object> map) {
        // 一年有效期
        return JWTUtil.createToken(map, JWT_SECRET.getBytes());
    }

    public static JSONObject parseJwtToken(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            // ================================================================================

            if (jwt.getPayload().getClaim(AuthConstant.JWT_UID_HEADER) == null
                    || jwt.getPayload().getClaim("imei") == null
                    || jwt.getPayload().getClaim("expireTime") == null) {
                return null;
            }
            // ================================================================================
            String expireTimeStr = jwt.getPayload().getClaim("expireTime").toString();
            if (expireTimeStr == null) {
                return null;
            }
            if (NumberUtil.isNumber(expireTimeStr)) {
                expireTimeStr = expireTimeStr + "000";
                expireTimeStr = DateUtil.date(Long.parseLong(expireTimeStr)).toString();
            }
            long currentTime = System.currentTimeMillis();
            long expireTime = DateUtil.parseDateTime(expireTimeStr).getTime();
            if (currentTime >= expireTime) {
                return null;
            }
            // ================================================================================
            // 判断imei和uid格式
            // ...
            // ================================================================================
            return jwt.getPayload().getClaimsJson();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 仅校验签名与必要 claims，不校验过期时间（供 token 续期的宽限判断使用）。
     * 签名无效或 claims 缺失返回 null；有效返回 claims JSON，
     * 其中 expireTime 为签发时的原始值（毫秒时间戳），是否过期/在宽限期内由调用方判断
     */
    public static JSONObject parseJwtTokenIgnoreExpiry(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            // 签名校验
            if (!jwt.setKey(JWT_SECRET.getBytes()).verify()) {
                return null;
            }
            if (jwt.getPayload().getClaim(AuthConstant.JWT_UID_HEADER) == null
                    || jwt.getPayload().getClaim("imei") == null
                    || jwt.getPayload().getClaim("expireTime") == null) {
                return null;
            }
            return jwt.getPayload().getClaimsJson();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {

    }

}
