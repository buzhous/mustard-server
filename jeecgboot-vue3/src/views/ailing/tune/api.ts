import { defHttp } from '/@/utils/http/axios';

enum Api {
  config = '/admin/ailing/tune/config',
  promptSave = '/admin/ailing/tune/prompt/save',
  promptRollback = '/admin/ailing/tune/prompt/rollback',
  promptRestore = '/admin/ailing/tune/prompt/restore',
  paramsSave = '/admin/ailing/tune/params/save',
  observeStats = '/admin/ailing/tune/observe/stats',
  observeMisses = '/admin/ailing/tune/observe/misses',
  aliasList = '/admin/ailing/tune/alias/list',
  aliasSave = '/admin/ailing/tune/alias/save',
  aliasDelete = '/admin/ailing/tune/alias/delete',
  feedbackList = '/admin/ailing/tune/feedback/list',
  feedbackHandle = '/admin/ailing/tune/feedback/handle',
}

/** 全量配置视图：{filing, query, model, search, history} */
export const getTuneConfig = () => defHttp.get({ url: Api.config }, { successMessageMode: 'none' });

/** 提示词保存（占位符缺失时服务端返回 error，message 含缺失占位符清单，errorMessageMode=none 由页面内联展示） */
export function savePrompt(data: { skill: string; content: string }) {
  return defHttp.post({ url: Api.promptSave, data }, { successMessageMode: 'none', errorMessageMode: 'none' });
}

/** 提示词一键回滚内置（该 skill 全部 DB 行停用，历史保留） */
export function rollbackPrompt(skill: string) {
  return defHttp.post({ url: Api.promptRollback, data: { skill } }, { successMessageMode: 'none' });
}

/** 恢复提示词历史版本（追加新 active 版本，审计可追溯） */
export function restorePrompt(data: { skill: string; version: number }) {
  return defHttp.post({ url: Api.promptRestore, data }, { successMessageMode: 'none' });
}

/** 模型与检索参数下发（均可空=不改），保存后即刻全节点生效 */
export function saveParams(data: { model?: Recordable; search?: Recordable }) {
  return defHttp.post({ url: Api.paramsSave, data }, { successMessageMode: 'none' });
}

/** 兜底链观测统计：{days, totalQueries, cachedHits, cacheHitRate, avgTotalMs, p95TotalMs, llmPickShare, stages:[{stage,cnt,hits,avgMs}]} */
export function getObserveStats(days: number) {
  return defHttp.get({ url: Api.observeStats, params: { days } }, { successMessageMode: 'none' });
}

/** 低命中样本列表（source_type=2 最近 200 条，下划线列名 ref_content/result_summary/sample_status/create_time/uid） */
export const getObserveMisses = () => defHttp.get({ url: Api.observeMisses }, { successMessageMode: 'none' });

/** 词表分页：{records, total}（下划线列名），alias/standard 模糊 + aliasType/enabled 筛选 */
export function getAliasList(params: Recordable) {
  return defHttp.get({ url: Api.aliasList, params }, { successMessageMode: 'none' });
}

/** 词表新增/更新：{id?, alias, standard, aliasType, enabled}；更新时 alias 不可改 */
export function saveAlias(data: { id?: string; alias: string; standard: string; aliasType: number; enabled: number }) {
  return defHttp.post({ url: Api.aliasSave, data }, { successMessageMode: 'none' });
}

/** 词表删除（物理删除，二次确认由前端承担） */
export const deleteAlias = (id: string) => defHttp.post({ url: Api.aliasDelete, data: { id } }, { successMessageMode: 'none' });

/** 样本池分页：{records, total}（下划线列名），sourceType/sampleStatus/uid/时间筛选 */
export function getFeedbackList(params: Recordable) {
  return defHttp.get({ url: Api.feedbackList, params }, { successMessageMode: 'none' });
}

/** 样本处置：alias=补词表 / ignore=忽略 / baseline=导出基准用例（result.case 为用例对象，前端 JSON 下载） */
export function handleFeedback(data: { id: string; action: 'alias' | 'ignore' | 'baseline'; standard?: string; aliasType?: number }) {
  return defHttp.post({ url: Api.feedbackHandle, data }, { successMessageMode: 'none' });
}
