import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/admin/ailing/item/list',
  detail = '/admin/ailing/item/detail',
  timeline = '/admin/ailing/item/timeline',
  stats = '/admin/ailing/item/stats',
  rebuildIndex = '/admin/ailing/item/searchIndex/rebuild',
  indexProgress = '/admin/ailing/item/searchIndex/progress',
}

/**
 * 物品列表
 * 服务端返回 {records, pageNo, pageSize, view}，无 total，前端按「下一页是否存在」本地估算 total
 */
export function getItemList(params?: Recordable) {
  return defHttp
    .get({ url: Api.list, params }, { successMessageMode: 'none' })
    .then((res: any) => {
      const records = res?.records ?? [];
      const pageNo = res?.pageNo ?? params?.pageNo ?? 1;
      const pageSize = res?.pageSize ?? params?.pageSize ?? 10;
      const base = (pageNo - 1) * pageSize;
      const total = records.length < pageSize ? base + records.length : base + pageSize + 1;
      return { records, total };
    });
}

/** 物品详情：正常物品全字段 或 {view:'recycle', recycle:{...}}；404 为 error */
export const getItemDetail = (params: Recordable) => defHttp.get({ url: Api.detail, params }, { successMessageMode: 'none' });

/** 流转时间线（含 chainValid / spawnChildren） */
export const getItemTimeline = (params: Recordable) => defHttp.get({ url: Api.timeline, params }, { successMessageMode: 'none' });

/** 物品统计 */
export const getItemStats = () => defHttp.get({ url: Api.stats }, { successMessageMode: 'none' });

/** 重建检索索引（异步任务，按钮权限 ailing:rebuild） */
export const rebuildSearchIndex = () => defHttp.post({ url: Api.rebuildIndex });

/** 检索索引重建进度 {running,total,done,fail} */
export const getSearchIndexProgress = () => defHttp.get({ url: Api.indexProgress }, { successMessageMode: 'none' });
