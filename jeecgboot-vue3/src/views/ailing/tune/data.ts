import { BasicColumn, FormSchema } from '/@/components/Table';

/** 调优 config_key（与 ai 模块 TuningConfigService 同串，版本历史按 key 过滤） */
export const FILING_PROMPT_KEY = 'skill.filing.prompt';
export const QUERY_PROMPT_KEY = 'skill.query.prompt';

/** 兜底链环节：①alias ②index ③keyword_as_category ④llm_pick ⑤category_drift ⑥relaxed + cache */
export const stageMap: Record<string, string> = {
  cache: '意图缓存',
  alias: '① 别名词表',
  index: '② 检索索引',
  keyword_as_category: '③ 关键词归类',
  llm_pick: '④ LLM 兜底',
  category_drift: '⑤ 类目漂移',
  relaxed: '⑥ 放宽召回',
};
export const stageColorMap: Record<string, string> = {
  cache: 'purple',
  alias: 'green',
  index: 'green',
  keyword_as_category: 'blue',
  llm_pick: 'orange',
  category_drift: 'gold',
  relaxed: 'red',
};

/** 词表类型：1 同义词 / 2 上下位 */
export const aliasTypeMap: Record<string, string> = { 1: '同义词', 2: '上下位' };
export const aliasTypeColorMap: Record<string, string> = { 1: 'blue', 2: 'purple' };
export const aliasTypeOptions = [1, 2].map((v) => ({ label: aliasTypeMap[String(v)], value: v }));

/** 样本状态：0 未处置 / 1 已处置 / 2 已忽略 */
export const sampleStatusMap: Record<string, string> = { 0: '未处置', 1: '已处置', 2: '已忽略' };
export const sampleStatusColorMap: Record<string, string> = { 0: 'orange', 1: 'green', 2: 'default' };

/** 样本来源：1 用户反馈 / 2 低命中自动样本 / 3 其他 */
export const sourceTypeMap: Record<string, string> = { 1: '用户反馈', 2: '低命中样本', 3: '其他反馈' };
export const sourceTypeColorMap: Record<string, string> = { 1: 'blue', 2: 'orange', 3: 'default' };
export const sourceTypeOptions = Object.keys(sourceTypeMap).map((v) => ({ label: sourceTypeMap[v], value: Number(v) }));
export const sampleStatusOptions = Object.keys(sampleStatusMap).map((v) => ({ label: sampleStatusMap[v], value: Number(v) }));

/** 观测窗口 */
export const daysOptions = [
  { label: '近 1 天', value: 1 },
  { label: '近 7 天', value: 7 },
  { label: '近 30 天', value: 30 },
];

/** 配置来源标记：DB 配置（绿）/ 内置默认（橙） */
export function sourceTag(source?: string): { color: string; text: string } {
  return source === 'db' ? { color: 'green', text: 'DB 配置' } : { color: 'orange', text: '内置默认' };
}

/** 提示词来源标记：DB vN / 内置回退 */
export function skillSourceTag(view: Recordable): { color: string; text: string } {
  return view?.source === 'db' && view?.version
    ? { color: 'green', text: `DB v${view.version}` }
    : { color: 'orange', text: '内置回退' };
}

/** 观测环节表列 */
export const stageColumns: BasicColumn[] = [
  { title: '环节', dataIndex: 'stage', key: 'stage', width: 160 },
  { title: '触发次数', dataIndex: 'cnt', width: 110, align: 'right' },
  { title: '命中次数', dataIndex: 'hits', width: 110, align: 'right' },
  { title: '命中率', dataIndex: 'hitRate', key: 'hitRate', width: 110, align: 'right' },
  { title: '平均耗时', dataIndex: 'avgMs', key: 'avgMs', width: 110, align: 'right' },
];

/** 低命中样本表列（tbl_ai_feedback 下划线列名） */
export const missColumns: BasicColumn[] = [
  { title: '查询词', dataIndex: 'ref_content', key: 'ref_content', width: 220, ellipsis: true },
  { title: '归因摘要', dataIndex: 'result_summary', key: 'result_summary', ellipsis: true },
  { title: '用户', dataIndex: 'username', key: 'username', width: 140, ellipsis: true },
  { title: '状态', dataIndex: 'sample_status', key: 'sample_status', width: 90 },
  { title: '时间', dataIndex: 'create_time', width: 170 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 250 },
];

/** 提示词版本历史表列（tbl_ai_tuning_config 各 key 历史行） */
export const historyColumns: BasicColumn[] = [
  { title: '版本', dataIndex: 'version', key: 'version', width: 80, align: 'right' },
  { title: '内容长度', dataIndex: 'valueLength', width: 100, align: 'right' },
  { title: '状态', dataIndex: 'rowState', key: 'rowState', width: 110 },
  { title: '备注', dataIndex: 'remark', width: 160, ellipsis: true },
  { title: '操作人', dataIndex: 'updateBy', width: 120, ellipsis: true },
  { title: '更新时间', dataIndex: 'updateTime', width: 170 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 100 },
];

/** 词表搜索表单 */
export const aliasSearchSchema: FormSchema[] = [
  { label: '别名', field: 'alias', component: 'Input', colProps: { span: 6 } },
  { label: '标准词', field: 'standard', component: 'Input', colProps: { span: 6 } },
  { label: '词型', field: 'aliasType', component: 'Select', componentProps: { options: aliasTypeOptions, allowClear: true }, colProps: { span: 6 } },
  {
    label: '启用',
    field: 'enabled',
    component: 'Select',
    componentProps: { options: [{ label: '启用', value: 1 }, { label: '停用', value: 0 }], allowClear: true },
    colProps: { span: 6 },
  },
];

/** 词表列表列 */
export const aliasColumns: BasicColumn[] = [
  { title: '别名', dataIndex: 'alias', width: 200, ellipsis: true },
  { title: '标准词', dataIndex: 'standard', width: 200, ellipsis: true },
  { title: '词型', dataIndex: 'alias_type', key: 'alias_type', width: 100 },
  { title: '启用', dataIndex: 'enabled', key: 'enabled', width: 90 },
  { title: '更新时间', dataIndex: 'update_time', width: 170 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 160 },
];

/** 观测环节命中率（hits/cnt，保留 1 位小数） */
export function stageHitRate(row: Recordable): string {
  const cnt = Number(row?.cnt) || 0;
  if (!cnt) return '-';
  return `${(((Number(row?.hits) || 0) * 100) / cnt).toFixed(1)}%`;
}

/** 版本历史行状态标记 */
export function historyRowState(row: Recordable): { color: string; text: string } {
  if (Number(row?.active) === 1 && Number(row?.enabled) === 1) return { color: 'green', text: '当前生效' };
  if (Number(row?.enabled) === 1) return { color: 'blue', text: 'DB 生效' };
  return { color: 'default', text: '已停用' };
}

/** 浏览器下载 JSON 文件（基准用例导出） */
export function downloadJson(filename: string, data: unknown) {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename || 'case.json';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
