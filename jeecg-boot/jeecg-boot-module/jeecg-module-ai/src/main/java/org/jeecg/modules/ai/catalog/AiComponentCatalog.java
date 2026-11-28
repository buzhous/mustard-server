package org.jeecg.modules.ai.catalog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * MIP v1 组件注册表 AI 侧瘦镜像（element 1-34）
 * <p>
 * 语义必须与 jeecg-module-app 的 MipComponentRegistry 保持一致，修改时必须双侧同步。
 * 用途：编排器 fields[] 白名单校验、restricted 拦截、valueType 宽松校验——
 * AI 模块不依赖 app 模块类型，故按模块隔离先例自建。
 * <p>
 * sensitivity 规则：默认值来自本注册表，Profile 只能 private → restricted 单向提级；
 * {@link #LOCKED_RESTRICTED} 中组件任何 Profile 均不得降级为 public，AI 一律不落值。
 */
public final class AiComponentCatalog {

    /** 组件形态：通用 / 虚拟物品专用 / 实物物品专用 */
    public static final String FORM_GENERAL = "general";
    public static final String FORM_VIRTUAL = "virtual";
    public static final String FORM_PHYSICAL = "physical";

    /** sensitivity 三级 */
    public static final String SENS_PUBLIC = "public";
    public static final String SENS_PRIVATE = "private";
    public static final String SENS_RESTRICTED = "restricted";

    /** 绝对 restricted：AI 不落值，仅提示手动补录 */
    public static final Set<Integer> LOCKED_RESTRICTED = Set.of(17, 18, 21, 22, 25, 26, 33);

    /** 允许声明为 public 的展示性组件 */
    public static final Set<Integer> PUBLIC_CAPABLE = Set.of(1, 2, 3, 4, 8, 10, 23, 27, 29);

    /** 已由草稿顶层槽位表达、fields[] 禁止重复写入的元素（7 数量与「件数=行数」语义冲突） */
    public static final Set<Integer> TOP_LEVEL_ELEMENTS = Set.of(1, 2, 3, 7, 8, 10, 11);

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

    private AiComponentCatalog() {
    }

    public static Component get(int element) {
        return COMPONENTS.get(element);
    }

    public static boolean isKnown(int element) {
        return COMPONENTS.containsKey(element);
    }

    public static String valueType(int element) {
        Component c = COMPONENTS.get(element);
        return c == null ? null : c.valueType();
    }

    public static boolean isRestricted(int element) {
        return LOCKED_RESTRICTED.contains(element);
    }

    /** 组件定义（不可变） */
    public record Component(int element, String name, String valueType,
                            String sensitivity, String form) {
    }
}
