import { defHttp } from '/@/utils/http/axios';
import { Modal } from 'ant-design-vue';

enum Api {
  list = '/warehouse/station/list',
  selectAll = '/warehouse/station/selectAll',
  queryById = '/warehouse/station/queryById',
  save = '/warehouse/station/add',
  edit = '/warehouse/station/edit',
  delete = '/warehouse/station/delete',
  deleteBatch = '/warehouse/station/deleteBatch',
  exportXls = '/warehouse/station/exportXls',
  importExcel = '/warehouse/station/importExcel',
}
/**
 * 导出/导入地址（供 useListPage 导出导入配置使用）
 */
export const getExportUrl = Api.exportXls;
export const getImportUrl = Api.importExcel;

/**
 * 分页列表查询
 * @param params
 */
export const list = (params) => {
  return defHttp.get({ url: Api.list, params });
};

/**
 * 全量列表查询（下拉选择等场景）
 * @param params
 */
export const selectAll = (params?) => {
  return defHttp.get({ url: Api.selectAll, params });
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
export const deleteStation = (params, handleSuccess) => {
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

/**
 * 导出excel（blob 方式下载）
 * @param params
 */
export const exportXls = (params?) => {
  return defHttp.get({ url: Api.exportXls, params, responseType: 'blob' });
};

/**
 * 导入excel
 * @param params 文件上传参数
 */
export const importExcel = (params) => {
  return defHttp.uploadFile({ url: Api.importExcel }, params);
};
