import { defHttp } from '/@/utils/http/axios';
import { Modal } from 'ant-design-vue';

enum Api {
  articleList = '/article/admin/list',
  articleQueryById = '/article/admin/queryById',
  articleAdd = '/article/admin/add',
  articleEdit = '/article/admin/edit',
  articleDelete = '/article/admin/delete',
  articleDeleteBatch = '/article/admin/deleteBatch',
  columnList = '/article/admin/column/list',
  columnAdd = '/article/admin/column/add',
  columnEdit = '/article/admin/column/edit',
  columnDelete = '/article/admin/column/delete',
}

/**
 * 文章分页列表
 * @param params
 */
export const list = (params) => {
  return defHttp.get({ url: Api.articleList, params });
};

/**
 * 文章详情（含富文本）
 * @param params
 */
export const queryById = (params) => {
  return defHttp.get({ url: Api.articleQueryById, params });
};

/**
 * 新增文章
 * @param params
 */
export const addArticle = (params) => {
  return defHttp.post({ url: Api.articleAdd, params });
};

/**
 * 编辑文章
 * @param params
 */
export const editArticle = (params) => {
  return defHttp.post({ url: Api.articleEdit, params });
};

/**
 * 单条删除文章
 * @param params
 */
export const deleteArticle = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.articleDelete, data: params }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};

/**
 * 批量删除文章
 * @param params
 */
export const deleteBatch = (params, handleSuccess) => {
  Modal.confirm({
    title: '确认删除',
    content: '是否删除选中的文章？删除后不可恢复',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      return defHttp.delete({ url: Api.articleDeleteBatch, data: params }, { joinParamsToUrl: true }).then(() => {
        handleSuccess();
      });
    },
  });
};

/**
 * 栏目列表
 */
export const columnList = (params?) => {
  return defHttp.get({ url: Api.columnList, params });
};

/**
 * 新增栏目
 * @param params
 */
export const addColumn = (params) => {
  return defHttp.post({ url: Api.columnAdd, params });
};

/**
 * 编辑栏目
 * @param params
 */
export const editColumn = (params) => {
  return defHttp.post({ url: Api.columnEdit, params });
};

/**
 * 删除栏目（栏目下存在文章时后端拒绝）
 * @param params
 */
export const deleteColumn = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.columnDelete, data: params }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};
