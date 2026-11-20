package org.jeecg.modules.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jeecg.modules.app.entity.ItemLineage;

/**
 * 物品流转账本 Mapper（append-only，仅 INSERT / SELECT）
 */
@Mapper
public interface ItemLineageMapper extends BaseMapper<ItemLineage> {
}
