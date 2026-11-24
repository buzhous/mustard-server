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
 * 分类三级匹配链（C3 分类治理）：
 * ① 用户分类精确/包含命中复用 → ② 系统分类精确/包含命中按系统分类实例化
 * （createCategory 回填 system_id，与用户关联，全局可聚合）→ ③ 都不匹配回退未分类
 * （AI 提名清单外名称不再自由创建，分类收敛到统一体系；用户手动建分类不受限）。
 * 标签仍为查重→创建两段（无系统体系）。异常回退 fallback=true，自动分类永不阻塞建档主链路。
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
     * 分类确保：用户分类复用 → 系统分类实例化 → 未分类。
     *
     * @param name 模型提名名（≤4 字强裁剪，须来自统一分类清单）
     * @param icon iconpark 图标提名（可空，缺省默认图标）
     * @return EnsureResult（fallback=true 表示清单外/创建失败，调用方应回退未分类）
     */
    public EnsureResult ensureCategory(String uid, String name, String icon) {
        String normalized = normalize(name);
        if (normalized == null) {
            return EnsureResult.fallbackResult();
        }
        try {
            // 统一清单：user=true 用户分类（含实例化产物）+ user=false 系统分类
            List<AiCategoryRef> unified = taxonomyPort.listUnifiedCategories(uid);
            // ① 用户分类精确命中复用
            for (AiCategoryRef c : unified) {
                if (c.isUser() && normalized.equalsIgnoreCase(c.getName())) {
                    return EnsureResult.reused(c.getId(), c.getName());
                }
            }
            // ② 用户分类包含命中复用（避免「书写工具」vs「书写」式重复）
            for (AiCategoryRef c : unified) {
                if (c.isUser() && c.getName() != null
                        && (c.getName().contains(normalized) || normalized.contains(c.getName()))) {
                    return EnsureResult.reused(c.getId(), c.getName());
                }
            }
            // ③ 系统分类精确/包含命中 → 为用户实例化（回填 system_id 与用户关联）
            for (AiCategoryRef s : unified) {
                if (!s.isUser() && s.getName() != null
                        && (s.getName().equalsIgnoreCase(normalized)
                        || s.getName().contains(normalized) || normalized.contains(s.getName()))) {
                    AiCategoryRef created = taxonomyPort.createCategory(uid, s.getName(), DEFAULT_CATEGORY_ICON, s.getId());
                    if (created == null || StrUtil.isBlank(created.getId())) {
                        return EnsureResult.fallbackResult();
                    }
                    return EnsureResult.fromSystem(created.getId(), created.getName());
                }
            }
            // ④ 清单外提名不再自由创建：回退未分类（分类收敛到统一体系）
            return EnsureResult.fallbackResult();
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
     * ensure 结果（record）：fallback=true 时 created=false 且 id 为 null；
     * fromSystem=true 表示按系统分类实例化（全局统一体系可聚合）
     */
    public record EnsureResult(String id, String name, boolean created, boolean fallback, boolean fromSystem) {

        static EnsureResult created(String id, String name) {
            return new EnsureResult(id, name, true, false, false);
        }

        static EnsureResult reused(String id, String name) {
            return new EnsureResult(id, name, false, false, false);
        }

        /** 系统分类实例化工厂（created=true, fromSystem=true） */
        static EnsureResult fromSystem(String id, String name) {
            return new EnsureResult(id, name, true, false, true);
        }

        /** fallback 工厂（避免与 record 访问器 fallback() 同名冲突） */
        static EnsureResult fallbackResult() {
            return new EnsureResult(null, null, false, true, false);
        }
    }
}
