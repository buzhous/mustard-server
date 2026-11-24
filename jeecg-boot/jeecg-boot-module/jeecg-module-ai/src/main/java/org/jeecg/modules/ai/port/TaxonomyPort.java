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
 */
public interface TaxonomyPort {

    /**
     * 用户分类列表（uid 精确匹配，无排序，与原 LambdaQueryWrapper 行为一致）
     */
    List<AiCategoryRef> listCategories(String uid);

    /**
     * 创建用户分类（id 服务端 ASSIGN_ID 生成；isSys=0；icon 可空）
     *
     * @return 创建后的分类引用（含 id/name）
     */
    AiCategoryRef createCategory(String uid, String name, String icon);

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
}
