package org.jeecg.modules.admin.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 用户仓库查看 后台 Mapper（App 用户信息查询）
 */
public interface UserWarehouseMapper {

    /**
     * 按手机号/用户名/用户Id 查找 App 用户
     *
     * @param key 手机号、用户名或用户Id
     * @return {id, phone, nickname}，未找到返回 null
     */
    @Select("SELECT id, phone, nickname FROM tbl_user WHERE phone = #{key} OR username = #{key} OR id = #{key} LIMIT 1")
    Map<String, Object> selectUserInfoByKey(@Param("key") String key);

    /**
     * 批量查询 App 用户信息
     *
     * @param userIds 用户Id列表
     * @return [{id, phone, nickname}]
     */
    @Select("<script>SELECT id, phone, nickname FROM tbl_user WHERE id IN "
            + "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach>"
            + "</script>")
    List<Map<String, Object>> selectUserInfos(@Param("userIds") List<String> userIds);
}
