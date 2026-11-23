import { defHttp } from '/@/utils/http/axios';

enum Api {
  overview = '/app/stats/admin/overview',
  trend = '/app/stats/admin/trend',
  events = '/app/stats/admin/events',
  retention = '/app/stats/admin/retention',
  exportCsv = '/app/stats/admin/export',
}

/**
 * KPI 总览：今日实时 + 近 N 天汇总 + 漏斗 + 注册来源
 * @param params {days}
 */
export const getOverview = (params) => {
  return defHttp.get({ url: Api.overview, params });
};

/**
 * 趋势序列（metric 如 dau/new_users/event:app_launch）
 * @param params {metric, dimension, days}
 */
export const getTrend = (params) => {
  return defHttp.get({ url: Api.trend, params });
};

/**
 * 原始事件明细分页（uid 已服务端假名化）
 * @param params {date, event, pageNo, pageSize}
 */
export const getEvents = (params) => {
  return defHttp.get({ url: Api.events, params });
};

/**
 * 留存矩阵（cohort 注册日后第 N 日活跃）
 * @param params {days}
 */
export const getRetention = (params) => {
  return defHttp.get({ url: Api.retention, params });
};

/**
 * CSV 导出（近 N 天日汇总，后端返回纯文本，需取原生响应）
 * @param params {days}
 */
export const exportStatsCsv = (params) => {
  return defHttp.get({ url: Api.exportCsv, params, responseType: 'text' }, { isReturnNativeResponse: true, isTransformResponse: false });
};
