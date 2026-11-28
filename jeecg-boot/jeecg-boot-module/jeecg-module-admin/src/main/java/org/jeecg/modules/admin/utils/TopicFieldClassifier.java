package org.jeecg.modules.admin.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 主题字段配置分类器
 * <p>
 * fields 规范（对齐 Apifox《物品数据结构》组件列表）：
 * [{element(1-20), name, sort, isShow, attributes, content}]
 * <p>
 * 组件类型：1图标 2名称 3类别 4等级 5购入时间 6价格 7数量 8描述 9特殊描述 10标签组
 * 11属性组 12图片 13视频 14文图视附件 15附件 16电子书 17密码 18定位 19富文本 20 3D模型
 */
public final class TopicFieldClassifier {

    /** 必填三要素：图标、名称、类别（element 1 图标 / 2 名称，与 tbl_field_element 字典一致） */
    public static final int ELEMENT_ICON = 1;
    public static final int ELEMENT_NAME = 2;
    public static final int ELEMENT_CATEGORY = 3;

    /** 主题类型 */
    public static final int TYPE_BASIC = 1;
    public static final int TYPE_MEDIA = 2;
    public static final int TYPE_MODEL = 3;
    public static final int TYPE_EXTENDED = 4;

    private TopicFieldClassifier() {
    }

    /**
     * 校验 fields 配置：必须为合法 JSON 数组，且包含图标(1)、名称(2)、类别(3)三个必填组件
     *
     * @return 校验失败原因；null 表示校验通过
     */
    public static String validate(String fieldsJson) {
        if (fieldsJson == null || fieldsJson.isBlank()) {
            return "字段配置不能为空，必须包含：图标(1)、名称(2)、类别(3)";
        }
        JSONArray array;
        try {
            array = JSONArray.parseArray(fieldsJson);
        } catch (Exception e) {
            return "字段配置不是合法的 JSON 数组";
        }
        if (array == null || array.isEmpty()) {
            return "字段配置不能为空，必须包含：图标(1)、名称(2)、类别(3)";
        }
        Set<Integer> elements = extractElements(array);
        for (int required : new int[]{ELEMENT_NAME, ELEMENT_ICON, ELEMENT_CATEGORY}) {
            if (!elements.contains(required)) {
                return "字段配置必须包含：图标(1)、名称(2)、类别(3)";
            }
        }
        return null;
    }

    /**
     * 根据字段配置自动推导主题类型（优先级：模型型 > 多媒体型 > 扩展型 > 基础型）
     */
    public static int classify(String fieldsJson) {
        Set<Integer> elements;
        try {
            elements = extractElements(JSONArray.parseArray(fieldsJson));
        } catch (Exception e) {
            return TYPE_BASIC;
        }
        if (elements.contains(20)) {
            return TYPE_MODEL;
        }
        if (elements.contains(12) || elements.contains(13) || elements.contains(14) || elements.contains(15)) {
            return TYPE_MEDIA;
        }
        if (elements.contains(17) || elements.contains(18) || elements.contains(19)) {
            return TYPE_EXTENDED;
        }
        return TYPE_BASIC;
    }

    private static Set<Integer> extractElements(JSONArray array) {
        Set<Integer> elements = new LinkedHashSet<>();
        if (array == null) {
            return elements;
        }
        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            if (obj == null) {
                continue;
            }
            Integer element = obj.getInteger("element");
            if (element != null) {
                elements.add(element);
            }
        }
        return elements;
    }
}
