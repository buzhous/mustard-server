package org.jeecg.modules.app.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.entity.KuaidiOrder;
import org.jeecg.modules.app.mapper.KuaidiOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 快递100 寄件下单服务
 * 配置 jeecg.kuaidi100.key/secret-code 后走真实下单接口；
 * key 为空时自动 mock（本地/E2E 用），下单即成功并返回模拟取件码
 *
 * @author elinx
 */
@Slf4j
@Service
public class Kuaidi100Service {

    /** 快递100 寄件下单接口 */
    private static final String SEND_ORDER_URL = "https://api.kuaidi100.com/v5/send/api/sendOrder";

    @Value("${jeecg.kuaidi100.key:}")
    private String secretKey;

    @Value("${jeecg.kuaidi100.code:}")
    private String secretCode;

    @Value("${jeecg.kuaidi100.callback-url:}")
    private String callbackUrl;

    @Value("${jeecg.kuaidi100.com:zhongtong}")
    private String kuaidicom;

    @Autowired
    private KuaidiOrderMapper kuaidiOrderMapper;

    public boolean isMock() {
        return StrUtil.isBlank(secretKey);
    }

    /**
     * 创建寄件订单（落 tbl_kuaidi_order）
     * real 模式：POST /v5/send/api/sendOrder，form: secret_key/secret_code/secret_sign=MD5(param+key)/param
     * mock 模式：直接生成模拟 taskId/快递单号/取件码
     */
    public KuaidiOrder createOrder(String recordId,
                                   String sendName, String sendPhone, String sendAddr,
                                   String recName, String recPhone, String recAddr) {
        Date now = new Date();
        KuaidiOrder order = new KuaidiOrder()
                .setRecordId(recordId)
                .setKuaidicom(kuaidicom)
                .setSendName(sendName)
                .setSendPhone(sendPhone)
                .setSendAddr(sendAddr)
                .setRecName(recName)
                .setRecPhone(recPhone)
                .setRecAddr(recAddr)
                .setOrderStatus(0)
                .setCreateTime(now)
                .setUpdateTime(now);
        if (isMock()) {
            order.setTaskId("MOCK" + IdUtil.fastSimpleUUID().substring(0, 16).toUpperCase())
                    .setKuaidinum("MOCK" + RandomUtil.randomNumbers(10))
                    .setPickCode(String.valueOf(RandomUtil.randomInt(1000, 9999)))
                    .setCallbackRaw("{\"mock\":true}");
        } else {
            JSONObject param = new JSONObject();
            param.set("kuaidicom", kuaidicom);
            param.set("sendMan", new JSONObject().set("name", sendName).set("tel", sendPhone).set("addr", sendAddr));
            param.set("recMan", new JSONObject().set("name", recName).set("tel", recPhone).set("addr", recAddr));
            if (StrUtil.isNotBlank(callbackUrl)) {
                param.set("callbackUrl", callbackUrl);
            }
            String paramStr = param.toString();
            Map<String, Object> form = new HashMap<>();
            form.put("secret_key", secretKey);
            form.put("secret_code", secretCode);
            form.put("secret_sign", SecureUtil.md5(paramStr + secretKey));
            form.put("param", paramStr);
            String resp;
            try {
                resp = HttpUtil.post(SEND_ORDER_URL, form, 10000);
            } catch (Exception e) {
                log.error("快递100下单请求异常 recordId={}", recordId, e);
                throw new AppException(ExceptionEnum.KUAIDI_ORDER_FAIL);
            }
            log.info("快递100下单响应 recordId={}, resp={}", recordId, resp);
            JSONObject json;
            try {
                json = JSONUtil.parseObj(resp);
            } catch (Exception e) {
                throw new AppException(ExceptionEnum.KUAIDI_ORDER_FAIL);
            }
            if (!json.getBool("success", false)) {
                order.setCallbackRaw(StrUtil.sub(resp, 0, 2000));
                kuaidiOrderMapper.insert(order);
                throw new AppException(ExceptionEnum.KUAIDI_ORDER_FAIL);
            }
            JSONObject data = json.getJSONObject("data");
            order.setTaskId(data == null ? null : data.getStr("taskId"))
                    .setKuaidinum(data == null ? null : data.getStr("kuaidinum"))
                    .setPickCode(data == null ? null : data.getStr("pickCode"))
                    .setCallbackRaw(StrUtil.sub(resp, 0, 2000));
        }
        kuaidiOrderMapper.insert(order);
        return order;
    }

    /**
     * 按 taskId 定位订单（回调入口）
     */
    public KuaidiOrder getByTaskId(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return null;
        }
        return kuaidiOrderMapper.selectOne(new LambdaQueryWrapper<KuaidiOrder>()
                .eq(KuaidiOrder::getTaskId, taskId)
                .orderByDesc(KuaidiOrder::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 按转入/移库单取最新快递订单
     */
    public KuaidiOrder getLatestByRecordId(String recordId) {
        if (StrUtil.isBlank(recordId)) {
            return null;
        }
        return kuaidiOrderMapper.selectOne(new LambdaQueryWrapper<KuaidiOrder>()
                .eq(KuaidiOrder::getRecordId, recordId)
                .orderByDesc(KuaidiOrder::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 更新订单状态（回调/轮询共用）
     */
    public KuaidiOrder updateOrderStatus(KuaidiOrder order, int status, String kuaidinum, String raw) {
        order.setOrderStatus(status);
        if (StrUtil.isNotBlank(kuaidinum)) {
            order.setKuaidinum(kuaidinum);
        }
        if (StrUtil.isNotBlank(raw)) {
            order.setCallbackRaw(StrUtil.sub(raw, 0, 2000));
        }
        order.setUpdateTime(new Date());
        kuaidiOrderMapper.updateById(order);
        return order;
    }
}
