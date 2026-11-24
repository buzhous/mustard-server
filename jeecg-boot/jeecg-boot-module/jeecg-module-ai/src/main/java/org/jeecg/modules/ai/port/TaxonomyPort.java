package org.jeecg.modules.ai.port;

import org.jeecg.modules.ai.bean.dto.AiCategoryRef;
import org.jeecg.modules.ai.bean.dto.AiStationRef;
import org.jeecg.modules.ai.bean.dto.AiTagRef;
import org.jeecg.modules.ai.bean.dto.AiTopicRef;

import java.util.List;

/**
 * 分类/标签/主题/仓位端口：ai 模块读取与自动创建（app 模块桥接实现）
 * <p>
 * 自动创建（createCategory/createTag）为 R4 受控工具的落库通道：
 * 编排查重（TaxonomyTools.ensureCategory/ensureTag）后调用，直接落
 * tbl_user_category / tbl_user_tag，与手动创建完全同构；失败由调用方回退，不阻塞建档。
 * <p>
 * C3 分类治理：用户分类（个人维度）+ 系统分类（全局统一）双体系——
 * 统一清单（listUnifiedCategories）供提示词与查询字典；系统分类命中时按
 * createCategory(uid,name,icon,systemId) 实例化为用户分类并回填来源关联。
 */
public interface TaxonomyPort {

    /**
     * 用户分类列表（uid 精确匹配，无排序，与原 LambdaQueryWrapper 行为一致）
     */
    List<AiCategoryRef> listCategories(String uid);

    /**
     * 统一分类清单（C3）：用户分类全部 + 启用系统分类（标记 user=false），
     * 系统分类若已被该用户实例化则带 userCategoryId（提示词据此区分「已有/系统」）
     */
    List<AiCategoryRef> listUnifiedCategories(String uid);

    /**
     * 创建用户分类（id 服务端 ASSIGN_ID 生成；icon 可空缺省）
     *
     * @param systemId 系统分类来源 id（实例化时传入，自由创建传 null）
     * @return 创建后的分类引用（含 id/name）
     */
    AiCategoryRef createCategory(String uid, String name, String icon, String systemId);

    /**
     * 用户标签列表（uid 精确匹配）
     */
    List<AiTagRef> listTags(String uid);

    /**
     * 创建用户标签（id 服务端 ASSIGN_ID 生成）
     */
    AiTagRef createTag(String uid, String name);

    /**
     * 平台启用主题模板列表（topicStatus=0），fields JSON 解析为组件编号集（仅保留已知组件）
     */
    List<AiTopicRef> listTopics();

    /**
     * 用户仓位列表：虚拟仓（1 个，type=1，名称=自定义名或「虚拟仓」）+ 已绑定真实站点仓（type=2）
     */
    List<AiStationRef> listStations(String uid);

    /**
     * 系统分类名 → 该用户已实例化的用户分类值集合（id+名称，供查询 categoryList in 过滤；
     * 未命中系统分类或无实例化行返回空集合）
     */
    List<String> categoryIdsBySystemName(String uid, String systemName);
}
