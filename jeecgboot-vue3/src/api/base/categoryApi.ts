import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/manage/system/category/list',
  add = '/manage/system/category/add',
  edit = '/manage/system/category/edit',
  status = '/manage/system/category/status',
}

/**
 * 系统分类分页列表（tbl_system_category，C3 分类治理）
 * @param params 支持 name 模糊筛选，按 sort 升序
 */
export const list = (params) => {
  return defHttp.get({ url: Api.list, params });
};

/**
 * 新增系统分类（名称全局唯一，后端校验）
 * @param params
 */
export const add = (params) => {
  return defHttp.post({ url: Api.add, params });
};

/**
 * 编辑系统分类（名称/图标/排序/状态/说明）
 * @param params
 */
export const edit = (params) => {
  return defHttp.post({ url: Api.edit, params });
};

/**
 * 停用/启用系统分类（停用不物理删除：已实例化的用户分类 system_id 仍指向该行）
 * @param params
 * @param handleSuccess
 */
export const changeStatus = (params, handleSuccess) => {
  return defHttp.post({ url: Api.status, params }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};
