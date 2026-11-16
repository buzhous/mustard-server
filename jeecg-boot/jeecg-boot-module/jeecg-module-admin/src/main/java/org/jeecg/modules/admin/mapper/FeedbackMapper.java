package org.jeecg.modules.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.admin.entity.feedback.Feedback;

import java.util.List;
import java.util.Map;

/**
 * 用户反馈 Mapper
 */
@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {

    /**
     * 批量查询提交用户信息（昵称/手机号），用于列表补充展示
     */
    @Select("<script>"
            + "SELECT id, nickname, phone FROM tbl_user WHERE id IN "
            + "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach>"
            + "</script>")
    List<Map<String, Object>> selectUserInfos(@Param("userIds") List<String> userIds);
}
