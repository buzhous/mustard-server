package org.jeecg.modules.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.app.entity.RechargeOrder;
import org.jeecg.modules.app.mapper.RechargeOrderMapper;
import org.jeecg.modules.app.service.IRechargeOrderService;
import org.springframework.stereotype.Service;


@Service
public class RechargeOrderServiceImpl extends ServiceImpl<RechargeOrderMapper, RechargeOrder> implements IRechargeOrderService {

}
