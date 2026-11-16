package org.jeecg.modules.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.admin.entity.coupon.CouponAdmin;
import org.jeecg.modules.admin.mapper.CouponAdminMapper;
import org.jeecg.modules.admin.service.ICouponAdminService;
import org.springframework.stereotype.Service;

/**
 * 优惠券模板 后台管理服务实现
 */
@Service
public class CouponAdminServiceImpl extends ServiceImpl<CouponAdminMapper, CouponAdmin> implements ICouponAdminService {
}
