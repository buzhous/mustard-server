package org.jeecg.modules.app.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.app.entity.FieldElement;
import org.jeecg.modules.app.mapper.FieldElementMapper;
import org.jeecg.modules.app.service.IBaseFieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 字段字典独立基础服务实现：
 * Redis 双键缓存（内容键 + 版本键，TTL 365 天），读路径优先缓存、Redis 异常降级直查 DB，
 * 写路径（saveElement / toggleStatus）事务落库后 refreshCacheAndRenew 同步刷新缓存并续期。
 */
@Slf4j
@Service
public class BaseFieldServiceImpl implements IBaseFieldService {

    public static final String PLATFORM_IOS = "ios";
    public static final String PLATFORM_ANDROID = "android";

    /** 缓存内容键：字典全量 JSON */
    private static final String CACHE_KEY_ELEMENTS = "mustard:base:field:elements";
    /** 缓存版本键：字典版本号 */
    private static final String CACHE_KEY_VERSION = "mustard:base:field:version";
    /** 缓存有效期：365 天（秒） */
    private static final long CACHE_TTL_SECONDS = 365L * 24 * 60 * 60;
    /** 必填三要素：1 图标 / 2 名称 / 3 类别 */
    private static final Set<Integer> REQUIRED_ELEMENTS = Set.of(1, 2, 3);
    /** 字段标识：英文驼峰 */
    private static final Pattern FIELD_KEY_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*$");
    /** 新增字段自动分配的起始编号 */
    private static final int EXTENSION_ELEMENT_BASE = 35;

    @Autowired
    private FieldElementMapper fieldElementMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<FieldElement> getActiveFields(String platform) {
        if (!PLATFORM_IOS.equals(platform) && !PLATFORM_ANDROID.equals(platform)) {
            throw new IllegalArgumentException("platform 仅支持 ios / android");
        }
        List<FieldElement> all = getAllFields();
        return all.stream()
                .filter(f -> f.getStatus() != null && f.getStatus() == 1)
                .filter(f -> PLATFORM_IOS.equals(platform)
                        ? (f.getIosFlag() != null && f.getIosFlag() == 1)
                        : (f.getAndroidFlag() != null && f.getAndroidFlag() == 1))
                .toList();
    }

    @Override
    public List<FieldElement> getAllFields() {
        try {
            Object cached = redisUtil.get(CACHE_KEY_ELEMENTS);
            if (cached != null) {
                return JSON.parseArray(cached.toString(), FieldElement.class);
            }
        } catch (Exception e) {
            log.warn("[fieldElement] 读缓存失败，降级直查 DB：{}", e.getMessage());
        }
        List<FieldElement> all = loadFromDb();
        tryRebuildCacheQuietly(all);
        return all;
    }

    @Override
    public FieldElement getById(String id) {
        return fieldElementMapper.selectById(id);
    }

    @Override
    public long getVersion() {
        Long version = readVersionQuietly();
        if (version == null) {
            // 缓存缺失（Redis 清空 / 首次部署）：以时间戳初始化，保证与任意历史版本都不相等，触发 App 全量拉取
            version = System.currentTimeMillis();
            try {
                redisUtil.set(CACHE_KEY_VERSION, String.valueOf(version), CACHE_TTL_SECONDS);
            } catch (Exception e) {
                log.warn("[fieldElement] 版本初始化写缓存失败：{}", e.getMessage());
            }
        }
        return version;
    }

    @Override
    public boolean isElementActive(Integer element) {
        if (element == null) {
            return false;
        }
        return getAllFields().stream()
                .anyMatch(f -> element.equals(f.getElement()) && f.getStatus() != null && f.getStatus() == 1);
    }

    @Override
    public FieldElement saveElement(FieldElement input) {
        // 基础校验
        if (input == null || StrUtil.isBlank(input.getName())) {
            throw new IllegalArgumentException("字段名称不能为空");
        }
        if (StrUtil.isBlank(input.getFieldKey()) || !FIELD_KEY_PATTERN.matcher(input.getFieldKey().trim()).matches()) {
            throw new IllegalArgumentException("字段标识必须为英文驼峰（字母开头，仅含字母数字）");
        }
        if (StrUtil.isBlank(input.getInputType())) {
            throw new IllegalArgumentException("输入类型不能为空");
        }
        input.setName(input.getName().trim());
        input.setFieldKey(input.getFieldKey().trim());
        input.setInputType(input.getInputType().trim());

        if (StrUtil.isBlank(input.getId())) {
            // 新增
            if (input.getElement() == null) {
                // 扩展编号自动顺延分配（当前最大编号 +1）
                input.setElement(maxElement() + 1);
            } else {
                if (input.getElement() <= 0) {
                    throw new IllegalArgumentException("组件编号必须为正整数");
                }
                if (countByElement(input.getElement(), null) > 0) {
                    throw new IllegalArgumentException("组件编号 " + input.getElement() + " 已被占用");
                }
            }
            if (countByFieldKey(input.getFieldKey(), null) > 0) {
                throw new IllegalArgumentException("字段标识 " + input.getFieldKey() + " 已存在");
            }
            lockRequiredThree(input);
            if (input.getStatus() == null) {
                input.setStatus(1);
            }
            if (input.getSort() == null) {
                input.setSort(input.getElement());
            }
            if (input.getAndroidFlag() == null) {
                input.setAndroidFlag(1);
            }
            if (input.getIosFlag() == null) {
                input.setIosFlag(1);
            }
            fieldElementMapper.insert(input);
        } else {
            // 编辑：组件编号锁定不可改，必填三要素锁定必填属性
            FieldElement exist = fieldElementMapper.selectById(input.getId());
            if (exist == null) {
                throw new IllegalArgumentException("未找到对应字段");
            }
            input.setElement(exist.getElement());
            lockRequiredThree(input);
            if (countByFieldKey(input.getFieldKey(), input.getId()) > 0) {
                throw new IllegalArgumentException("字段标识 " + input.getFieldKey() + " 已存在");
            }
            fieldElementMapper.updateById(input);
        }
        refreshCacheAndRenew();
        return input;
    }

    @Override
    public void toggleStatus(String id, Integer status) {
        if (StrUtil.isBlank(id) || status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("参数错误");
        }
        FieldElement update = new FieldElement();
        update.setId(id);
        update.setStatus(status);
        int rows = fieldElementMapper.updateById(update);
        if (rows == 0) {
            throw new IllegalArgumentException("未找到对应字段");
        }
        refreshCacheAndRenew();
    }

    @Override
    public void refreshCacheAndRenew() {
        List<FieldElement> all = loadFromDb();
        long nextVersion;
        Long current = readVersionQuietly();
        nextVersion = (current == null ? 0L : current) + 1;
        try {
            redisUtil.set(CACHE_KEY_ELEMENTS, JSON.toJSONString(all), CACHE_TTL_SECONDS);
            redisUtil.set(CACHE_KEY_VERSION, String.valueOf(nextVersion), CACHE_TTL_SECONDS);
            log.info("[fieldElement] 缓存已刷新，共 {} 条，版本 {}，TTL 续期 365 天", all.size(), nextVersion);
        } catch (Exception e) {
            // Redis 异常不影响主流程：DB 已更新，下次读缓存缺失自动降级直查 DB 重建
            log.error("[fieldElement] 刷新缓存失败（DB 已更新，降级为下次读时重建）：{}", e.getMessage());
        }
    }

    // ==================== 内部方法 ====================

    private List<FieldElement> loadFromDb() {
        List<FieldElement> list = fieldElementMapper.selectList(
                new QueryWrapper<FieldElement>().orderByAsc("sort").orderByAsc("element"));
        return list == null ? Collections.emptyList() : list;
    }

    private void tryRebuildCacheQuietly(List<FieldElement> all) {
        try {
            redisUtil.set(CACHE_KEY_ELEMENTS, JSON.toJSONString(all), CACHE_TTL_SECONDS);
        } catch (Exception e) {
            log.warn("[fieldElement] 重建缓存失败：{}", e.getMessage());
        }
    }

    private Long readVersionQuietly() {
        try {
            Object cached = redisUtil.get(CACHE_KEY_VERSION);
            if (cached != null) {
                return Long.parseLong(cached.toString());
            }
        } catch (Exception e) {
            log.warn("[fieldElement] 读版本缓存失败：{}", e.getMessage());
        }
        return null;
    }

    private int maxElement() {
        QueryWrapper<FieldElement> qw = new QueryWrapper<>();
        qw.select("IFNULL(MAX(element), 0)");
        List<Object> objs = fieldElementMapper.selectObjs(qw);
        if (objs.isEmpty() || objs.get(0) == null) {
            return 0;
        }
        return ((Number) objs.get(0)).intValue();
    }

    private long countByElement(Integer element, String excludeId) {
        QueryWrapper<FieldElement> qw = new QueryWrapper<>();
        qw.eq("element", element);
        if (excludeId != null) {
            qw.ne("id", excludeId);
        }
        return fieldElementMapper.selectCount(qw);
    }

    private long countByFieldKey(String fieldKey, String excludeId) {
        QueryWrapper<FieldElement> qw = new QueryWrapper<>();
        qw.eq("field_key", fieldKey);
        if (excludeId != null) {
            qw.ne("id", excludeId);
        }
        return fieldElementMapper.selectCount(qw);
    }

    private void lockRequiredThree(FieldElement input) {
        if (input.getElement() != null && REQUIRED_ELEMENTS.contains(input.getElement())) {
            input.setIsRequired(1);
        }
    }
}
