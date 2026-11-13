package org.jeecg.modules.admin.entity.warehouse;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;


@Data
@TableName("tbl_warehouse_station")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "实物云仓-仓储站点(平台收寄点)")
public class WarehouseStation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private String id;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    private Date updateTime;

    @Excel(name = "站点编码", width = 15)
    @Schema(description = "站点编码")
    private String stationCode;

    @Excel(name = "站点名称", width = 15)
    @Schema(description = "站点名称")
    private String stationName;

    @Excel(name = "省份", width = 15)
    @Schema(description = "省份(行政区域编码)")
    private String province;

    @Excel(name = "城市", width = 15)
    @Schema(description = "城市(行政区域编码)")
    private String city;

    @Excel(name = "区县", width = 15)
    @Schema(description = "区县(行政区域编码)")
    private String district;

    @Excel(name = "详细地址", width = 15)
    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Excel(name = "覆盖城市/区域", width = 15)
    @Schema(description = "覆盖城市/区域")
    private String coverArea;

    @Excel(name = "可存储品类", width = 15)
    @Schema(description = "可存储品类")
    private String storageCategories;

    @Excel(name = "仓储标准", width = 15)
    @Schema(description = "仓储标准")
    private String warehouseStandard;

    @Schema(description = "收费标准JSON")
    private String feeStandard;

    @Excel(name = "配送覆盖范围", width = 15)
    @Schema(description = "配送覆盖范围")
    private String deliveryScope;

    @Excel(name = "上门取件范围", width = 15)
    @Schema(description = "上门取件范围")
    private String pickupScope;

    @Schema(description = "预计时效(天)")
    private Integer estimateDays;

    @Schema(description = "库存总容量(件)")
    private Integer totalCapacity;

    @Schema(description = "已用容量(件)")
    private Integer usedCapacity;

    @Excel(name = "营业时间", width = 15)
    @Schema(description = "营业时间")
    private String businessHours;

    @Excel(name = "联系电话", width = 15)
    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "站点图片")
    private String stationImage;

    @Schema(description = "状态:0停用,1开放")
    private Integer status;

    @Schema(description = "排序")
    private Integer sort;
}
