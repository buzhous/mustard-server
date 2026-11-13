import { defHttp } from '/@/utils/http/axios';
import { Modal } from 'ant-design-vue';

enum Api {
  list = '/topic/admin/list',
  queryById = '/topic/admin/queryById',
  save = '/topic/admin/add',
  edit = '/topic/admin/edit',
  delete = '/topic/admin/delete',
  deleteBatch = '/topic/admin/deleteBatch',
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
 * 新增
 * @param params
 */
export const add = (params) => {
  return defHttp.post({ url: Api.save, params });
};

/**
 * 编辑
 * @param params
 */
export const edit = (params) => {
  return defHttp.put({ url: Api.edit, params });
};

/**
 * 单条删除
 * @param params
 */
export const deleteTopic = (params, handleSuccess) => {
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
    content: '是否删除选中数据',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      return defHttp.delete({ url: Api.deleteBatch, data: params }, { joinParamsToUrl: true }).then(() => {
        handleSuccess();
      });
    },
  });
};
