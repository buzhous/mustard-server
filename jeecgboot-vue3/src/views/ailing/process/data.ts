import { BasicColumn, FormSchema } from '/@/components/Table';

/** 会话模式：normal 对话建档 / express 快速建档 / query 查询会话 */
export const modeMap: Record<string, string> = { normal: '普通建档', express: '快速建档', query: '查询' };
export const modeColorMap: Record<string, string> = { normal: 'green', express: 'gold', query: 'default' };
export const modeOptions = Object.keys(modeMap).map((v) => ({ label: modeMap[v], value: v }));

/** 会话最终状态：active 进行中 / filed 已入库 */
export const sessionStatusMap: Record<string, string> = { active: '进行中', filed: '已入库' };
export const sessionStatusColorMap: Record<string, string> = { active: 'orange', filed: 'green' };
export const sessionStatusOptions = Object.keys(sessionStatusMap).map((v) => ({ label: sessionStatusMap[v], value: v }));

export const columns: BasicColumn[] = [
  { title: '用户', dataIndex: 'username', key: 'username', width: 120 },
  { title: '标题', dataIndex: 'title', key: 'title', width: 200 },
  { title: '消息数', dataIndex: 'message_count', width: 80, align: 'right' },
  { title: '模式', dataIndex: 'mode', key: 'mode', width: 100 },
  { title: '最终状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '整体置信度', dataIndex: 'last_confidence', key: 'last_confidence', width: 100, align: 'right' },
  { title: '最近更新', dataIndex: 'update_time', width: 170 },
];

export const searchFormSchema: FormSchema[] = [
  { label: '会话ID', field: 'sessionId', component: 'Input', colProps: { span: 6 } },
  { label: '用户ID', field: 'uid', component: 'Input', colProps: { span: 6 } },
  { label: '用户名', field: 'username', component: 'Input', colProps: { span: 6 } },
  { label: '标题', field: 'title', component: 'Input', colProps: { span: 6 } },
  { label: '最终状态', field: 'status', component: 'Select', componentProps: { options: sessionStatusOptions, allowClear: true }, colProps: { span: 6 } },
  { label: '模式', field: 'mode', component: 'Select', componentProps: { options: modeOptions, allowClear: true }, colProps: { span: 6 } },
  { label: '创建时间', field: 'createTime', component: 'RangePicker', colProps: { span: 6 } },
];

/** 消息角色 */
export function isUserMsg(role?: string): boolean {
  return role === 'user';
}

/** 解析 JSON 字符串字段（非法/为空返回 null） */
export function parseJsonField(raw: any): any {
  if (raw === null || raw === undefined || raw === '') return null;
  if (typeof raw === 'object') return raw;
  try {
    return JSON.parse(String(raw));
  } catch {
    return null;
  }
}

/** 草稿卡槽位取值（{value, confidence} 或裸值） */
export function slotValue(slot: any): string {
  if (slot === null || slot === undefined) return '';
  if (typeof slot === 'object') return String(slot.value ?? '');
  return String(slot);
}

/** 草稿标签数组（槽位数组 / 字符串数组 / 空格分隔字符串） */
export function draftTags(tags: any): string[] {
  if (!tags) return [];
  const arr = Array.isArray(tags) ? tags : [];
  const out: string[] = [];
  for (const t of arr) {
    const v = typeof t === 'object' ? String(t?.value ?? '') : String(t);
    if (v) out.push(v);
  }
  if (!arr.length && typeof tags === 'string') {
    out.push(...tags.split(/\s+/).filter(Boolean));
  }
  return [...new Set(out)];
}

/** 查询结果卡摘要（type=search_items/get_item/get_item_lineage/get_stats） */
export function queryCardSummary(card: any): { type: string; rows: string[] } {
  if (!card || typeof card !== 'object') return { type: '-', rows: [] };
  const type = String(card.type ?? '-');
  const rows: string[] = [];
  const pick = (o: any, keys: string[]) => keys.map((k) => (o?.[k] ?? '')).filter((x) => x !== '').join(' / ');
  if (type === 'search_items' && Array.isArray(card.rows)) {
    for (const r of card.rows.slice(0, 5)) rows.push(pick(r, ['name', 'category', 'price', 'quantity']));
  } else if (type === 'get_item' && card.item) {
    rows.push(pick(card.item, ['name', 'category', 'price', 'quantity']));
  } else if (type === 'get_item_lineage' && Array.isArray(card.events)) {
    for (const e of card.events.slice(0, 5)) rows.push(pick(e, ['event_type', 'operator', 'qty']));
  } else if (type === 'get_stats' && card.stats) {
    rows.push(JSON.stringify(card.stats).slice(0, 160));
  }
  return { type, rows };
}
