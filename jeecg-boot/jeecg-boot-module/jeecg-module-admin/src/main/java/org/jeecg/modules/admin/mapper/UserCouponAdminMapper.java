package org.jeecg.modules.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.admin.entity.coupon.UserCouponAdmin;

import java.util.List;
import java.util.Map;

/**
 * @Description: tbl_user_coupon 后台管理
 */
public interface UserCouponAdminMapper extends BaseMapper<UserCouponAdmin> {

    /**
     * 按手机号/用户名/用户Id 查找 App 用户（tbl_user）
     *
     * @param key 手机号、用户名或用户Id
     * @return 用户Id，未找到返回 null
     */
    @Select("SELECT id FROM tbl_user WHERE phone = #{key} OR username = #{key} OR id = #{key} LIMIT 1")
    String selectUserIdByKey(@Param("key") String key);

    /**
     * 批量查询 App 用户昵称（持券列表展示用）
     *
     * @param userIds 用户Id列表
     * @return [{id, nickname}]
     */
    @Select("<script>SELECT id, nickname FROM tbl_user WHERE id IN "
            + "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach>"
            + "</script>")
    List<Map<String, Object>> selectUserNicknames(@Param("userIds") List<String> userIds);
}
