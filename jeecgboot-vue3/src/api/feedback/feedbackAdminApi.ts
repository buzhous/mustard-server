import { defHttp } from '/@/utils/http/axios';
import { Modal } from 'ant-design-vue';

enum Api {
  list = '/feedback/admin/list',
  queryById = '/feedback/admin/queryById',
  reply = '/feedback/admin/reply',
  delete = '/feedback/admin/delete',
  deleteBatch = '/feedback/admin/deleteBatch',
}

/**
 * 反馈分页列表查询
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
 * 回复反馈（回复内容写入并标记已处理）
 * @param params {id, reply}
 */
export const replyFeedback = (params) => {
  return defHttp.post({ url: Api.reply, params });
};

/**
 * 单条删除
 * @param params
 */
export const deleteFeedback = (params, handleSuccess) => {
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
    content: '是否删除选中的用户反馈？删除后不可恢复',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      return defHttp.delete({ url: Api.deleteBatch, data: params }, { joinParamsToUrl: true }).then(() => {
        handleSuccess();
      });
    },
  });
};
