package org.jeecg.modules.admin.bean.vo.warehouse;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jeecg.modules.admin.entity.warehouse.UserStorage;
import org.jeecg.modules.admin.entity.warehouse.UserVirtualStation;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户仓库查看 VO（虚拟仓 + 自有仓 + 官方绑定仓）
 */
@Data
@Accessors(chain = true)
@Schema(description = "用户仓库查看")
public class UserWarehouseDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户信息")
    private UserInfo user;

    @Schema(description = "虚拟仓（固定 1 个）")
    private UserVirtualStation virtual;

    @Schema(description = "自有真实仓列表")
    private List<UserStorage> storages;

    @Schema(description = "官方绑定仓列表")
    private List<BindInfo> binds;

    @Data
    @Schema(description = "用户信息")
    public static class UserInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "用户Id")
        private String id;

        @Schema(description = "手机号")
        private String phone;

        @Schema(description = "昵称")
        private String nickname;
    }

    @Data
    @Schema(description = "官方绑定仓信息")
    public static class BindInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "绑定记录Id")
        private String id;

        @Schema(description = "站点Id")
        private String stationId;

        @Schema(description = "站点名称")
        private String stationName;

        @Schema(description = "站点编码")
        private String stationCode;

        @Schema(description = "自定义仓库名")
        private String customName;

        @Schema(description = "是否主站点:0否,1是")
        private Integer isDefault;

        @Schema(description = "状态:0已解绑,1已绑定")
        private Integer status;

        @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "绑定时间")
        private Date bindTime;
    }
}
