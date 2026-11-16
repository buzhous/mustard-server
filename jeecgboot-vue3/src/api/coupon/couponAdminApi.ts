import { defHttp } from '/@/utils/http/axios';
import { Modal } from 'ant-design-vue';

enum Api {
  list = '/coupon/admin/list',
  queryById = '/coupon/admin/queryById',
  add = '/coupon/admin/add',
  edit = '/coupon/admin/edit',
  delete = '/coupon/admin/delete',
  deleteBatch = '/coupon/admin/deleteBatch',
  publishBatch = '/coupon/admin/publishBatch',
  offlineBatch = '/coupon/admin/offlineBatch',
  adjustStock = '/coupon/admin/adjustStock',
}

enum UserCouponApi {
  list = '/coupon/admin/userCoupon/list',
  grant = '/coupon/admin/userCoupon/grant',
  invalidate = '/coupon/admin/userCoupon/invalidate',
}

/**
 * 优惠券分页列表查询
 * @param params
 */
export const list = (params) => {
  return defHttp.get({ url: Api.list, params });
};

/**
 * 优惠券详情
 * @param params
 */
export const queryById = (params) => {
  return defHttp.get({ url: Api.queryById, params });
};

/**
 * 新增优惠券
 * @param params
 */
export const add = (params) => {
  return defHttp.post({ url: Api.add, params });
};

/**
 * 编辑优惠券（剩余数量/已使用数量/状态不可修改）
 * @param params
 */
export const edit = (params) => {
  return defHttp.put({ url: Api.edit, params });
};

/**
 * 删除优惠券（已有用户持券记录的券禁止删除）
 * @param params
 */
export const deleteCoupon = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.delete, data: params }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};

/**
 * 批量删除优惠券（已有用户持券记录的券禁止删除）
 * @param params
 */
export const deleteBatch = (params, handleSuccess) => {
  Modal.confirm({
    title: '确认删除',
    content: '是否删除选中的优惠券？已有用户持券记录的券会被拒绝删除',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      return defHttp.delete({ url: Api.deleteBatch, data: params }, { joinParamsToUrl: true }).then(() => {
        handleSuccess();
      });
    },
  });
};

/**
 * 批量发布（未发布/已下架 → 已发布）
 * @param params
 */
export const publishBatch = (params) => {
  return defHttp.put({ url: Api.publishBatch, data: params }, { joinParamsToUrl: true });
};

/**
 * 批量下架（已发布 → 已下架）
 * @param params
 */
export const offlineBatch = (params) => {
  return defHttp.put({ url: Api.offlineBatch, data: params }, { joinParamsToUrl: true });
};

/**
 * 库存调整（正数增加、负数减少）
 * @param params {id, delta}
 */
export const adjustStock = (params) => {
  return defHttp.put({ url: Api.adjustStock, data: params }, { joinParamsToUrl: true });
};

/**
 * 用户持券分页列表查询
 * @param params
 */
export const userCouponList = (params) => {
  return defHttp.get({ url: UserCouponApi.list, params });
};

/**
 * 发放优惠券给指定用户
 * @param params {userKey, couponId, count}
 */
export const grantCoupon = (params) => {
  return defHttp.post({ url: UserCouponApi.grant, params }, { joinParamsToUrl: true });
};

/**
 * 批量作废未使用持券（回补库存）
 * @param params
 */
export const invalidateBatch = (params) => {
  return defHttp.put({ url: UserCouponApi.invalidate, data: params }, { joinParamsToUrl: true });
};
