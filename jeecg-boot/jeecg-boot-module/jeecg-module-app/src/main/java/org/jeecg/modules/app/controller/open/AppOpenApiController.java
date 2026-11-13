package org.jeecg.modules.app.controller.open;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.bean.vo.item.ItemInfoQueryVO;
import org.jeecg.modules.app.entity.ItemInfo;
import org.jeecg.modules.app.entity.OpenAppCredential;
import org.jeecg.modules.app.service.IItemInfoService;
import org.jeecg.modules.app.service.IOpenAppCredentialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 芥子开放平台数据接口（/app/open/**）
 *
 * 鉴权方式：AK/SK 签名（与 jeecg 网关 /openapi/call 一致）
 * - 请求头：appkey / timestamp / signature
 * - signature = md5(appkey + appSecret + timestamp)（小写）
 * - timestamp 与服务器时间偏差不超过 5 分钟
 *
 * 数据范围：凭据绑定的用户（open_app_credentials.bound_uid）的物品数据。
 * 该路径不走 AppAuthFilter（App 登录态），由本控制器独立验签。
 */
@Slf4j
@RestController
@RequestMapping("/app/open")
@Tag(name = "Open-开放平台数据接口")
public class AppOpenApiController {

    private static final long SIGN_EXPIRE_MILLIS = 5 * 60 * 1000L;

    @Autowired
    private IOpenAppCredentialService openAppCredentialService;

    @Autowired
    private IItemInfoService itemInfoService;

    @Operation(summary = "物品分页查询")
    @GetMapping("/item/list")
    public Result<Page<ItemInfo>> itemList(
            HttpServletRequest request,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "stationId", required = false) String stationId,
            @RequestParam(name = "stationType", required = false) Integer stationType,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "orderBy", required = false) String orderBy,
            @RequestParam(name = "ascending", required = false, defaultValue = "false") Boolean ascending) {
        Result<OpenAppCredential> authResult = checkSignature(request);
        if (!authResult.isSuccess()) {
            return Result.error(authResult.getMessage());
        }
        String boundUid = authResult.getResult().getBoundUid();
        if (StrUtil.isEmpty(boundUid)) {
            return Result.error("凭据未绑定数据范围用户，请联系平台管理员");
        }
        ItemInfoQueryVO queryVO = new ItemInfoQueryVO()
                .setUserId(boundUid)
                .setKeyword(keyword)
                .setStationId(stationId)
                .setStationType(stationType)
                .setStatus(status)
                .setOrderBy(orderBy)
                .setAscending(ascending)
                .setPageNo(pageNo)
                .setPageSize(Math.min(pageSize, 100));
        return Result.OK(itemInfoService.listByCondition(queryVO));
    }

    @Operation(summary = "物品详情")
    @GetMapping("/item/detail")
    public Result<ItemInfo> itemDetail(
            HttpServletRequest request,
            @RequestParam(name = "itemId") String itemId) {
        Result<OpenAppCredential> authResult = checkSignature(request);
        if (!authResult.isSuccess()) {
            return Result.error(authResult.getMessage());
        }
        if (StrUtil.isEmpty(itemId)) {
            return Result.error("itemId 不能为空");
        }
        String boundUid = authResult.getResult().getBoundUid();
        ItemInfo itemInfo = itemInfoService.queryItemInfo(itemId);
        if (ObjectUtil.isEmpty(itemInfo) || !boundUid.equals(itemInfo.getUserId())) {
            return Result.error("物品不存在或无权访问");
        }
        return Result.OK(itemInfo);
    }

    /**
     * AK/SK 签名校验
     */
    private Result<OpenAppCredential> checkSignature(HttpServletRequest request) {
        String appKey = request.getHeader("appkey");
        String signature = request.getHeader("signature");
        String timestamp = request.getHeader("timestamp");
        if (StrUtil.hasEmpty(appKey, signature, timestamp)) {
            return Result.error("缺少 appkey/timestamp/signature 请求头");
        }
        long ts;
        try {
            ts = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException e) {
            return Result.error("timestamp 格式错误");
        }
        long offset = Math.abs(System.currentTimeMillis() - ts);
        if (offset > SIGN_EXPIRE_MILLIS) {
            return Result.error("signature 已过期（时间偏差超过 5 分钟），服务器时间："
                    + DateUtil.format(DateUtil.date(), DatePattern.NORM_DATETIME_PATTERN));
        }
        OpenAppCredential credential = openAppCredentialService.getEnabledByAppKey(appKey);
        if (ObjectUtil.isEmpty(credential)) {
            return Result.error("无效的 appkey 或凭据已停用");
        }
        String expected = SecureUtil.md5(appKey.trim() + credential.getAppSecret() + timestamp.trim());
        if (!expected.equalsIgnoreCase(signature.trim())) {
            return Result.error("signature 校验失败");
        }
        // IP 白名单
        String whiteList = credential.getWhiteList();
        if (StrUtil.isNotBlank(whiteList)) {
            String clientIp = resolveClientIp(request);
            boolean matched = StrUtil.split(whiteList, ',').stream()
                    .anyMatch(ip -> ip.trim().equals(clientIp));
            if (!matched) {
                return Result.error("IP 未授权：" + clientIp);
            }
        }
        return Result.OK(credential);
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
