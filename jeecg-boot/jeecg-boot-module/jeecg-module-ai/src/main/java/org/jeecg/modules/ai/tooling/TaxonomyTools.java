package org.jeecg.modules.ai.tooling;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ai.bean.dto.AiCategoryRef;
import org.jeecg.modules.ai.bean.dto.AiTagRef;
import org.jeecg.modules.ai.port.TaxonomyPort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * R4 受控原子工具：分类/标签自动创建（模型只提名，本工具执行并守全部业务约束）
 * <p>
 * 守约顺序：归一化（≤4 字强裁剪）→ 查重（精确/包含命中复用既有实体，不重复建）
 * → 创建（经 TaxonomyPort 落库）→ 异常回退（返回 fallback=true，调用方回退
 * 未分类/丢弃标签，自动分类永不阻塞建档主链路）。
 */
@Slf4j
@Component
public class TaxonomyTools {

    /** 提名名称上限（产品规则：分类/标签名 ≤4 字） */
    private static final int MAX_NAME_LEN = 4;
    /** 创建失败/异常时的默认图标（iconpark 图标名） */
    private static final String DEFAULT_CATEGORY_ICON = "box";

    private final TaxonomyPort taxonomyPort;

    public TaxonomyTools(TaxonomyPort taxonomyPort) {
        this.taxonomyPort = taxonomyPort;
    }

    /**
     * 分类确保：复用优先于创建。
     *
     * @param name 模型提名名（≤4 字强裁剪）
     * @param icon iconpark 图标提名（可空，缺省默认图标）
     * @return EnsureResult（fallback=true 表示创建失败，调用方应回退未分类）
     */
    public EnsureResult ensureCategory(String uid, String name, String icon) {
        String normalized = normalize(name);
        if (normalized == null) {
            return EnsureResult.fallbackResult();
        }
        try {
            List<AiCategoryRef> existing = taxonomyPort.listCategories(uid);
            // 1) 精确命中复用
            for (AiCategoryRef c : existing) {
                if (normalized.equalsIgnoreCase(c.getName())) {
                    return EnsureResult.reused(c.getId(), c.getName());
                }
            }
            // 2) 包含命中复用（避免「书写工具」vs「书写」式重复）
            for (AiCategoryRef c : existing) {
                if (c.getName() != null && (c.getName().contains(normalized) || normalized.contains(c.getName()))) {
                    return EnsureResult.reused(c.getId(), c.getName());
                }
            }
            // 3) 创建
            AiCategoryRef created = taxonomyPort.createCategory(uid, normalized,
                    StrUtil.blankToDefault(icon, DEFAULT_CATEGORY_ICON));
            if (created == null || StrUtil.isBlank(created.getId())) {
                return EnsureResult.fallbackResult();
            }
            return EnsureResult.created(created.getId(), created.getName());
        } catch (Exception e) {
            log.warn("[ai-tools] ensureCategory 失败 uid={} name={} 回退未分类: {}", uid, normalized, e.getMessage());
            return EnsureResult.fallbackResult();
        }
    }

    /**
     * 标签确保：复用优先于创建（每件 ≤3 截断由编排器执行）。
     */
    public EnsureResult ensureTag(String uid, String name) {
        String normalized = normalize(name);
        if (normalized == null) {
            return EnsureResult.fallbackResult();
        }
        try {
            List<AiTagRef> existing = taxonomyPort.listTags(uid);
            for (AiTagRef t : existing) {
                if (normalized.equalsIgnoreCase(t.getName())) {
                    return EnsureResult.reused(t.getId(), t.getName());
                }
            }
            for (AiTagRef t : existing) {
                if (t.getName() != null && (t.getName().contains(normalized) || normalized.contains(t.getName()))) {
                    return EnsureResult.reused(t.getId(), t.getName());
                }
            }
            AiTagRef created = taxonomyPort.createTag(uid, normalized);
            if (created == null || StrUtil.isBlank(created.getId())) {
                return EnsureResult.fallbackResult();
            }
            return EnsureResult.created(created.getId(), created.getName());
        } catch (Exception e) {
            log.warn("[ai-tools] ensureTag 失败 uid={} name={} 丢弃该标签: {}", uid, normalized, e.getMessage());
            return EnsureResult.fallbackResult();
        }
    }

    /** 提名归一化：trim + ≤4 字强裁剪；空值返回 null */
    private String normalize(String name) {
        String s = StrUtil.trimToNull(name);
        if (s == null) {
            return null;
        }
        return s.length() <= MAX_NAME_LEN ? s : s.substring(0, MAX_NAME_LEN);
    }

    /**
     * ensure 结果（record）：fallback=true 时 created=false 且 id 为 null
     */
    public record EnsureResult(String id, String name, boolean created, boolean fallback) {

        static EnsureResult created(String id, String name) {
            return new EnsureResult(id, name, true, false);
        }

        static EnsureResult reused(String id, String name) {
            return new EnsureResult(id, name, false, false);
        }

        /** fallback 工厂（避免与 record 访问器 fallback() 同名冲突） */
        static EnsureResult fallbackResult() {
            return new EnsureResult(null, null, false, true);
        }
    }
}
