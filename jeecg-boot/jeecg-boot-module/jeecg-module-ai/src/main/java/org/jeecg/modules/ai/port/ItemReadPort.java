package org.jeecg.modules.ai.port;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.ai.bean.dto.AiItemQuery;
import org.jeecg.modules.ai.bean.dto.AiItemRow;

import java.util.List;

/**
 * 物品读端口：ai 模块查询物品（app 模块桥接实现，底层透传 AppOpenService / IItemInfoService）
 */
public interface ItemReadPort {

    /**
     * 物品统计（总数/按状态/按分类/总价值）
     */
    JSONObject stats(String uid);

    /**
     * 物品详情（不存在返回 null）
     */
    JSONObject itemDetail(String uid, String itemId);

    /**
     * 流转记录（不存在返回 null）
     */
    JSONObject lineage(String uid, String itemId);

    /**
     * 条件分页查询物品（与原 IItemInfoService.listByCondition(ItemInfoQueryVO) 行为一致）
     */
    Page<AiItemRow> listByCondition(AiItemQuery query);

    /**
     * 按 itemId 查询物品（不存在返回 null）
     */
    AiItemRow queryByItemId(String uid, String itemId);

    /**
     * 别名扩展（P2-6 检索新链①）：词表命中返回 [原词, 标准词...]，未命中返回仅含原词的单元素列表
     */
    List<String> aliasExpand(String term);

    /**
     * 检索索引查询（P2-6 检索新链①②）：terms 任一命中全文索引（ngram），
     * filter 其余条件（category/tag/level/status/price/时间/仓位/排序分页）同步生效
     */
    Page<AiItemRow> searchIndexQuery(String uid, List<String> terms, AiItemQuery filter);

    /**
     * 用户物品实际在用的分类值列表（原 distinctItemCategories 的 LambdaQueryWrapper 查询：
     * select(category).eq(userId).isNotNull(category).ne(category,"")，仅返回 category 字段，顺序与原 list 一致）
     */
    List<String> listItemCategories(String uid);
}
