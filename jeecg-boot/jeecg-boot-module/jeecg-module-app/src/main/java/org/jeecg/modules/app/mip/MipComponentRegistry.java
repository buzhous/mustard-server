package org.jeecg.modules.app.mip;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * MIP v1 组件注册表（element 1-34）
 * <p>
 * 语义来源：jeecg-module-admin 的 TopicFieldClassifier（1-20 通用组件）+ MIP 协议 3.1/3.2 扩展
 * （21/22 证件专用，23-28 V 系列虚拟物品，29-34 P 系列实物）。
 * <p>
 * 模块隔离约束：jeecg-module-app 与 jeecg-module-admin 无相互依赖，故注册表在 app 模块内自建，
 * 语义与 admin 侧保持一致，修改时必须双侧同步。
 * <p>
 * sensitivity 规则（MIP 协议 3.1 注①②③ / 10.3 缺陷③）：
 * - 默认值来自本注册表，Profile 只能 private → restricted 单向提级；
 * - {@link #LOCKED_RESTRICTED} 中组件任何 Profile 均不得降级为 public；
 * - Profile 含 element 21（证件类）时，该 Profile 内 element 12 自动提级 restricted。
 */
public final class MipComponentRegistry {

    /** 组件形态：通用 / 虚拟物品专用 / 实物物品专用 */
    public static final String FORM_GENERAL = "general";
    public static final String FORM_VIRTUAL = "virtual";
    public static final String FORM_PHYSICAL = "physical";

    /** sensitivity 三级 */
    public static final String SENS_PUBLIC = "public";
    public static final String SENS_PRIVATE = "private";
    public static final String SENS_RESTRICTED = "restricted";

    /** 绝对 restricted：账号/兑换码/购买凭证等同密码性质，任何 Profile 不得降级 */
    public static final Set<Integer> LOCKED_RESTRICTED = Set.of(17, 18, 21, 22, 25, 26, 33);

    /** 允许声明为 public 的展示性组件（其余组件 Profile 声明 public 一律拒绝） */
    public static final Set<Integer> PUBLIC_CAPABLE = Set.of(1, 2, 3, 4, 8, 10, 23, 27, 29);

    /** 证件号组件：Profile 含 21 时 element 12 自动提级 restricted */
    public static final int ELEMENT_CERT_NO = 21;
    public static final int ELEMENT_IMAGES = 12;

    /** value 类型枚举：string/url/int/number/date/daterange/arrstr/arrobj/arrurl/obj/html */
    private static final Map<Integer, Component> COMPONENTS;

    static {
        Map<Integer, Component> m = new LinkedHashMap<>();
        // ===== 通用组件 1-22（对齐 TopicFieldClassifier 与 MIP 协议 3.1）=====
        m.put(1, new Component(1, "图标", "url", SENS_PUBLIC, FORM_GENERAL));
        m.put(2, new Component(2, "名称", "string", SENS_PUBLIC, FORM_GENERAL));
        m.put(3, new Component(3, "类别", "int", SENS_PUBLIC, FORM_GENERAL));
        m.put(4, new Component(4, "等级", "int", SENS_PUBLIC, FORM_GENERAL));
        m.put(5, new Component(5, "购入时间", "date", SENS_PRIVATE, FORM_GENERAL));
        m.put(6, new Component(6, "价格", "number", SENS_PRIVATE, FORM_GENERAL));
        m.put(7, new Component(7, "数量", "int", SENS_PRIVATE, FORM_GENERAL));
        m.put(8, new Component(8, "描述", "string", SENS_PUBLIC, FORM_GENERAL));
        m.put(9, new Component(9, "特殊描述", "string", SENS_PRIVATE, FORM_GENERAL));
        m.put(10, new Component(10, "标签组", "arrstr", SENS_PUBLIC, FORM_GENERAL));
        m.put(11, new Component(11, "属性组", "arrobj", SENS_PRIVATE, FORM_GENERAL));
        m.put(12, new Component(12, "图片", "arrurl", SENS_PRIVATE, FORM_GENERAL));
        m.put(13, new Component(13, "视频", "arrurl", SENS_PRIVATE, FORM_GENERAL));
        m.put(14, new Component(14, "文图视附件", "arrurl", SENS_PRIVATE, FORM_GENERAL));
        m.put(15, new Component(15, "附件", "arrurl", SENS_PRIVATE, FORM_GENERAL));
        m.put(16, new Component(16, "电子书", "url", SENS_PRIVATE, FORM_GENERAL));
        m.put(17, new Component(17, "密码", "string", SENS_RESTRICTED, FORM_GENERAL));
        m.put(18, new Component(18, "定位", "obj", SENS_RESTRICTED, FORM_GENERAL));
        m.put(19, new Component(19, "富文本", "html", SENS_PRIVATE, FORM_GENERAL));
        m.put(20, new Component(20, "3D模型", "url", SENS_PRIVATE, FORM_GENERAL));
        m.put(21, new Component(21, "证件号", "string", SENS_RESTRICTED, FORM_GENERAL));
        m.put(22, new Component(22, "有效期范围", "daterange", SENS_RESTRICTED, FORM_GENERAL));
        // ===== V 系列 · 虚拟物品 23-28（MIP 协议 3.2）=====
        m.put(23, new Component(23, "发行方/平台", "string", SENS_PUBLIC, FORM_VIRTUAL));
        m.put(24, new Component(24, "权益有效期", "daterange", SENS_PRIVATE, FORM_VIRTUAL));
        m.put(25, new Component(25, "账号标识", "string", SENS_RESTRICTED, FORM_VIRTUAL));
        m.put(26, new Component(26, "兑换码/激活码", "string", SENS_RESTRICTED, FORM_VIRTUAL));
        m.put(27, new Component(27, "链上凭证", "obj", SENS_PUBLIC, FORM_VIRTUAL));
        m.put(28, new Component(28, "云端存放位置", "url", SENS_PRIVATE, FORM_VIRTUAL));
        // ===== P 系列 · 实物物品 29-34（MIP 协议 3.2）=====
        m.put(29, new Component(29, "品牌/型号", "obj", SENS_PUBLIC, FORM_PHYSICAL));
        m.put(30, new Component(30, "序列号/SN", "string", SENS_PRIVATE, FORM_PHYSICAL));
        m.put(31, new Component(31, "成色评级", "int", SENS_PRIVATE, FORM_PHYSICAL));
        m.put(32, new Component(32, "保修信息", "obj", SENS_PRIVATE, FORM_PHYSICAL));
        m.put(33, new Component(33, "购买凭证", "arrurl", SENS_RESTRICTED, FORM_PHYSICAL));
        m.put(34, new Component(34, "维护记录", "arrobj", SENS_PRIVATE, FORM_PHYSICAL));
        COMPONENTS = Collections.unmodifiableMap(m);
    }

    private MipComponentRegistry() {
    }

    public static Component get(int element) {
        return COMPONENTS.get(element);
    }

    public static boolean isKnown(int element) {
        return COMPONENTS.containsKey(element);
    }

    public static String valueType(int element) {
        Component c = COMPONENTS.get(element);
        return c == null ? null : c.valueType;
    }

    public static String defaultSensitivity(int element) {
        Component c = COMPONENTS.get(element);
        return c == null ? null : c.defaultSensitivity;
    }

    public static String form(int element) {
        Component c = COMPONENTS.get(element);
        return c == null ? null : c.form;
    }

    /**
     * Profile 的 item_form（virtual/physical/both）是否允许启用该组件
     */
    public static boolean formAllowed(String profileForm, int element) {
        String f = form(element);
        if (f == null || FORM_GENERAL.equals(f)) {
            return true;
        }
        if ("both".equals(profileForm)) {
            return true;
        }
        return f.equals(profileForm);
    }

    /** 组件定义（不可变） */
    public record Component(int element, String name, String valueType,
                            String defaultSensitivity, String form) {
    }
}
