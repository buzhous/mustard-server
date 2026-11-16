import { defHttp } from '/@/utils/http/axios';
import { Modal } from 'ant-design-vue';

enum Api {
  list = '/ruins/admin/list',
  queryById = '/ruins/admin/queryById',
  delete = '/ruins/admin/delete',
  deleteBatch = '/ruins/admin/deleteBatch',
  offShelfBatch = '/ruins/admin/offShelfBatch',
  assignPickupBatch = '/ruins/admin/assignPickupBatch',
}

/**
 * 分页列表查询
 * @param params
 */
export const list = (params) => {
  return defHttp.get({ url: Api.list, params });
};

/**
 * 查询详情
 * @param params
 */
export const queryById = (params) => {
  return defHttp.get({ url: Api.queryById, params });
};

/**
 * 单条删除
 * @param params
 */
export const deleteRuins = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.delete, data: params }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};

/**
 * 批量删除
 * @param params
 */
export const deleteBatch = (params, handleSuccess) => {
  Modal.confirm({
    title: '确认删除',
    content: '是否删除选中的殷墟物品？删除后不可恢复',
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
 * 批量下架（仅待拾取状态可下架）
 * @param params
 */
export const offShelfBatch = (params) => {
  return defHttp.put({ url: Api.offShelfBatch, data: params }, { joinParamsToUrl: true });
};

/**
 * 批量指定用户拾取
 * @param params {ids, userKey}
 */
export const assignPickupBatch = (params) => {
  return defHttp.put({ url: Api.assignPickupBatch, data: params }, { joinParamsToUrl: true });
};
