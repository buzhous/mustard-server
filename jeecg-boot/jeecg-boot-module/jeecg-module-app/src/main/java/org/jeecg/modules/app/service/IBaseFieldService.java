package org.jeecg.modules.app.service;

import org.jeecg.modules.app.entity.FieldElement;

import java.util.List;

/**
 * 独立基础服务：字段字典（跨模块共享，admin → app → ai，admin 已依赖 app 无需新增依赖）
 * <p>
 * 读路径全部走 Redis 缓存（双键：内容键 mustard:base:field:elements / 版本键 mustard:base:field:version，
 * TTL 均 365 天），Redis 异常时降级直查 DB；写路径（saveElement / toggleStatus）落库后
 * 自动 {@link #refreshCacheAndRenew()} 刷新缓存、版本 +1 并续期。
 */
public interface IBaseFieldService {

    /**
     * 启用且对应端已勾选的字段列表（读缓存，按 sort 升序，扩展字段天然排末尾）
     *
     * @param platform ios / android（与 AppVersion 口径一致）
     */
    List<FieldElement> getActiveFields(String platform);

    /** 全量字段（含停用），按 sort 升序 —— 后台字段管理页列表使用 */
    List<FieldElement> getAllFields();

    /** 单条详情 —— 抽屉编辑回填 */
    FieldElement getById(String id);

    /** 当前字典版本号 —— App init 下发比对（两端共用，端标记调整同样版本 +1） */
    long getVersion();

    /** 单字段有效性判断（启用即有效，不区分端）—— 主题保存校验、解析链路使用 */
    boolean isElementActive(Integer element);

    /**
     * 新增 / 编辑字典唯一入口：
     * 校验 element 全局唯一（内置 1-34、扩展 35+ 任意未占用正整数；新增 element 为空时自动顺延分配）、
     * fieldKey 英文驼峰；编辑时锁定组件编号与必填三要素（1/2/3）属性；
     * 写库后自动刷新缓存。校验失败抛 {@link IllegalArgumentException}（msg 为用户可读文案）。
     */
    FieldElement saveElement(FieldElement element);

    /** 启用 / 停用字典字段（不提供删除：已录入/已引用的字段删除会导致主题配置与物品数据悬空） */
    void toggleStatus(String id, Integer status);

    /** 刷新缓存 + 版本 +1 + 两键 TTL 重置 365 天（所有写操作后自动触发） */
    void refreshCacheAndRenew();
}
