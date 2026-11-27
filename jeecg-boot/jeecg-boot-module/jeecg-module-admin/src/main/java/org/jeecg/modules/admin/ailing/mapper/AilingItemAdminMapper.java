package org.jeecg.modules.admin.ailing.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 芥灵AI 物品工作台 · 物品/回收站/统计只读 Mapper（admin 模块自建 SQL，不依赖 app 模块实体）
 */
public interface AilingItemAdminMapper {

    String NORMAL_FILTER = "<if test='itemId != null and itemId != \"\"'> AND i.id = #{itemId}</if>"
            + "<if test='keyword != null and keyword != \"\"'> AND (i.name like concat('%',#{keyword},'%') OR i.description like concat('%',#{keyword},'%'))</if>"
            + "<if test='category != null and category != \"\"'> AND i.category = #{category}</if>"
            + "<if test='tag != null and tag != \"\"'> AND i.fields like concat('%',#{tag},'%')</if>"
            + "<if test='level != null'> AND i.level = #{level}</if>"
            + "<if test='status != null'> AND i.status = #{status}</if>"
            + "<if test='stationId != null and stationId != \"\"'> AND i.station_id = #{stationId}</if>"
            + "<if test='priceMin != null'> AND i.price &gt;= #{priceMin}</if>"
            + "<if test='priceMax != null'> AND i.price &lt;= #{priceMax}</if>"
            + "<if test='createTimeStart != null'> AND i.create_time &gt;= #{createTimeStart}</if>"
            + "<if test='createTimeEnd != null'> AND i.create_time &lt;= #{createTimeEnd}</if>"
            + "<if test='source != null and source != \"\"'> AND i.source = #{source}</if>"
            + "<if test='uid != null and uid != \"\"'> AND i.user_id = #{uid}</if>"
            + "<if test='oriId != null and oriId != \"\"'> AND i.ori_id = #{oriId}</if>";

    /**
     * 在库物品分页（view=normal）：tbl_item_info + 存在未销毁同步记录过滤（与 App 检索口径一致）
     */
    @Select("<script>SELECT CAST(i.id AS CHAR) AS id, i.ori_id, i.name, i.icon, i.category, i.fields, i.level, i.quantity, "
            + "i.price, i.status, i.source, i.user_id, i.station_id, i.station_type, i.create_time, i.update_time "
            + "FROM tbl_item_info i "
            + "WHERE EXISTS (SELECT 1 FROM tbl_item_sync s WHERE s.item_id = CAST(i.id AS CHAR) COLLATE utf8mb4_general_ci "
            + "AND s.user_id = i.user_id AND (s.status IS NULL OR s.status &lt;&gt; 3)) "
            + NORMAL_FILTER
            + "<choose><when test=\"orderBy == 'name'\"> ORDER BY i.name ${orderDir}</when>"
            + "<when test=\"orderBy == 'price'\"> ORDER BY i.price ${orderDir}</when>"
            + "<when test=\"orderBy == 'quantity'\"> ORDER BY i.quantity ${orderDir}</when>"
            + "<when test=\"orderBy == 'status'\"> ORDER BY i.status ${orderDir}</when>"
            + "<otherwise> ORDER BY i.create_time ${orderDir}</otherwise></choose>"
            + " LIMIT #{page.offset}, #{page.size}"
            + "</script>")
    List<Map<String, Object>> selectNormalPage(PageArgs page, @Param("itemId") String itemId,
                                               @Param("keyword") String keyword, @Param("category") String category,
                                               @Param("tag") String tag, @Param("level") Integer level,
                                               @Param("status") Integer status, @Param("stationId") String stationId,
                                               @Param("priceMin") java.math.BigDecimal priceMin,
                                               @Param("priceMax") java.math.BigDecimal priceMax,
                                               @Param("createTimeStart") Date createTimeStart,
                                               @Param("createTimeEnd") Date createTimeEnd,
                                               @Param("source") String source, @Param("uid") String uid,
                                               @Param("oriId") String oriId,
                                               @Param("orderBy") String orderBy,
                                               @Param("orderDir") String orderDir);

    /**
     * 回收站物品分页（view=recycle）：tbl_item_recycle 直查（status=0 在站内，不入索引）
     */
    @Select("<script>SELECT CAST(r.id AS CHAR) AS id, r.ori_id, r.item_id, r.name, r.icon, r.category_id AS category, r.quantity, "
            + "r.user_id, r.station_id, r.status, r.create_time "
            + "FROM tbl_item_recycle r WHERE r.status = 0 "
            + "<if test='itemId != null and itemId != \"\"'> AND r.item_id = #{itemId}</if>"
            + "<if test='keyword != null and keyword != \"\"'> AND r.name like concat('%',#{keyword},'%')</if>"
            + "<if test='category != null and category != \"\"'> AND r.category_id = #{category}</if>"
            + "<if test='uid != null and uid != \"\"'> AND r.user_id = #{uid}</if>"
            + "<if test='oriId != null and oriId != \"\"'> AND r.ori_id = #{oriId}</if>"
            + "<if test='createTimeStart != null'> AND r.create_time &gt;= #{createTimeStart}</if>"
            + "<if test='createTimeEnd != null'> AND r.create_time &lt;= #{createTimeEnd}</if>"
            + " ORDER BY r.create_time DESC "
            + "</script>")
    List<Map<String, Object>> selectRecyclePage(PageArgs page, @Param("itemId") String itemId,
                                                @Param("keyword") String keyword, @Param("category") String category,
                                                @Param("uid") String uid, @Param("oriId") String oriId,
                                                @Param("createTimeStart") Date createTimeStart,
                                                @Param("createTimeEnd") Date createTimeEnd);

    @Select("SELECT CAST(i.id AS CHAR) AS id, i.* FROM tbl_item_info i WHERE i.id = #{itemId}")
    Map<String, Object> selectItemDetail(@Param("itemId") String itemId);

    @Select("SELECT r.* FROM tbl_item_recycle r WHERE r.item_id = #{itemId} AND r.status = 0 ORDER BY r.create_time DESC LIMIT 1")
    Map<String, Object> selectRecycleByItemId(@Param("itemId") String itemId);

    // ==================== 统计聚合（P2-4 stats：索引表 + tbl_item_info） ====================

    @Select("SELECT count(*) FROM tbl_item_info")
    Long countItems();

    @Select("SELECT count(DISTINCT user_id) FROM tbl_item_info")
    Long countDistinctUsers();

    @Select("SELECT count(*) FROM tbl_item_info WHERE create_time >= #{start}")
    Long countCreatedSince(@Param("start") Date start);

    @Select("SELECT count(*) FROM tbl_item_info WHERE source = 'ai'")
    Long countAiCreated();

    @Select("SELECT category_norm AS k, count(*) AS c FROM tbl_item_search_index "
            + "WHERE category_norm IS NOT NULL AND category_norm != '' "
            + "GROUP BY category_norm ORDER BY c DESC LIMIT 10")
    List<Map<String, Object>> categoryTop();

    @Select("SELECT tag_norms FROM tbl_item_search_index WHERE tag_norms IS NOT NULL AND tag_norms != '' LIMIT 10000")
    List<String> selectAllTagNorms();

    @Select("SELECT level AS k, count(*) AS c FROM tbl_item_info WHERE level IS NOT NULL GROUP BY level ORDER BY k ASC")
    List<Map<String, Object>> levelDist();

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS k, count(*) AS c FROM tbl_item_info "
            + "WHERE create_time >= #{start} GROUP BY k ORDER BY k ASC")
    List<Map<String, Object>> createdTrend(@Param("start") Date start);

    @Select("SELECT IFNULL(source, 'manual') AS k, count(*) AS c FROM tbl_item_info GROUP BY k ORDER BY c DESC")
    List<Map<String, Object>> sourceDist();

    /**
     * MP 分页占位：Mapper 方法首参为任意类型分页参数载体（Page），此处仅承载 pageNo/pageSize，
     * 由 Service 手工拼 LIMIT（避免引入分页插件对 Map 结果的强依赖）
     */
    class PageArgs {
        private final long offset;
        private final long size;

        public PageArgs(long pageNo, long pageSize) {
            long size = pageSize <= 0 ? 10 : Math.min(pageSize, 100);
            long no = pageNo <= 0 ? 1 : pageNo;
            this.offset = (no - 1) * size;
            this.size = size;
        }

        public long getOffset() {
            return offset;
        }

        public long getSize() {
            return size;
        }
    }
}
