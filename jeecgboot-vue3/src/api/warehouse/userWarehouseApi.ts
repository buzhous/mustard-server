import { defHttp } from '/@/utils/http/axios';

enum Api {
  page = '/warehouse/userStorage/page',
  detail = '/warehouse/userStorage/detail',
}

/**
 * 自有仓分页列表（userKey 支持手机号/用户名/用户Id）
 * @param params
 */
export const page = (params) => {
  return defHttp.get({ url: Api.page, params });
};

/**
 * 用户仓库详情（虚拟仓 + 自有仓 + 官方绑定仓）
 * @param params {userKey}
 */
export const detail = (params) => {
  return defHttp.get({ url: Api.detail, params });
};
