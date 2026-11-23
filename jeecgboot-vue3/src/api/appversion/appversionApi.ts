import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/app/version/admin/list',
  save = '/app/version/admin/save',
  rollback = '/app/version/admin/rollback',
  delete = '/app/version/admin/delete',
}

/**
 * App 版本管理（版本管理策略 §04）：双平台 latest 独立，强更门禁/灰度在服务端判定
 */
export const listAppVersion = (params?: Recordable) => defHttp.get({ url: Api.list, params });

export const saveAppVersion = (data: Recordable) => defHttp.post({ url: Api.save, data });

export const rollbackAppVersion = (data: { platform: string; targetId: number }) =>
  defHttp.post({ url: Api.rollback, data });

export const deleteAppVersion = (data: { id: number }) => defHttp.post({ url: Api.delete, data });
