package org.jeecg.modules.app.bean.vo.gift;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 赠送领取明细VO（多人领取记录）
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "赠送领取明细VO")
public class ItemGiftReceiveVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "领取记录ID")
    private String id;

    @Schema(description = "领取者用户ID")
    private String userId;

    @Schema(description = "领取者昵称")
    private String nickname;

    @Schema(description = "领取者头像")
    private String avatar;

    @Schema(description = "领取数量")
    private Integer quantity;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "领取时间")
    private Date receiveTime;

}
