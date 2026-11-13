package org.jeecg.modules.app.bean.vo.station;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "仓储站点VO")
public class WarehouseStationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "站点Id")
    private String id;

    @Schema(description = "站点编码")
    private String stationCode;

    @Schema(description = "站点名称")
    private String stationName;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "区县")
    private String district;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "覆盖城市/区域")
    private String coverArea;

    @Schema(description = "可存储品类")
    private String storageCategories;

    @Schema(description = "仓储标准")
    private String warehouseStandard;

    @Schema(description = "收费标准JSON")
    private String feeStandard;

    @Schema(description = "配送覆盖范围")
    private String deliveryScope;

    @Schema(description = "上门取件范围")
    private String pickupScope;

    @Schema(description = "预计时效(天)")
    private Integer estimateDays;

    @Schema(description = "库存总容量(件)")
    private Integer totalCapacity;

    @Schema(description = "已用容量(件)")
    private Integer usedCapacity;

    @Schema(description = "剩余可用容量(件)")
    private Integer availableCapacity;

    @Schema(description = "营业时间")
    private String businessHours;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "站点图片")
    private String stationImage;

    @Schema(description = "状态:0停用,1开放")
    private Integer status;
}
