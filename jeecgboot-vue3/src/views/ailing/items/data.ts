import { BasicColumn, FormSchema } from '/@/components/Table';

/** 物品状态：0 临时 1 正常 2 使用中 */
export const statusMap: Record<string, string> = { 0: '临时', 1: '正常', 2: '使用中' };
export const statusColorMap: Record<string, string> = { 0: 'default', 1: 'green', 2: 'blue' };

/** 建档来源 */
export const sourceMap: Record<string, string> = { manual: '手动建档', ai: 'AI建档', mcp: 'MCP接入', import: '导入' };
export const sourceColorMap: Record<string, string> = { manual: 'default', ai: 'purple', mcp: 'cyan', import: 'orange' };

/** 流转事件类型中文映射 */
export const eventMap: Record<string, string> = {
  mint: '铸造',
  gift_out: '赠出',
  gift_in: '受赠',
  ruins_drop: '丢殷墟',
  ruins_pick: '拾取',
  transfer: '流转',
  spawn: '派生',
  destroy: '销毁',
  recycle: '回收',
  import: '导入',
  update: '更新',
};
/** 时间线节点颜色 */
export const eventColorMap: Record<string, string> = {
  mint: 'green',
  gift_out: 'blue',
  gift_in: 'blue',
  ruins_drop: 'orange',
  ruins_pick: 'orange',
  transfer: 'blue',
  spawn: 'purple',
  destroy: 'red',
  recycle: 'gray',
  import: 'cyan',
  update: 'default',
};

export const viewOptions = [
  { label: '在库物品', value: 'normal' },
  { label: '回收站', value: 'recycle' },
];
export const sourceOptions = Object.keys(sourceMap).map((v) => ({ label: sourceMap[v], value: v }));
export const statusOptions = Object.keys(statusMap).map((v) => ({ label: statusMap[v], value: Number(v) }));
export const orderByOptions = [
  { label: '建档时间', value: 'create_time' },
  { label: '更新时间', value: 'update_time' },
  { label: '价格', value: 'price' },
  { label: '等级', value: 'level' },
  { label: '数量', value: 'quantity' },
];
export const orderDirOptions = [
  { label: '降序', value: 'desc' },
  { label: '升序', value: 'asc' },
];

export const columns: BasicColumn[] = [
  { title: '名称', dataIndex: 'name', key: 'name', width: 240 },
  { title: '分类', dataIndex: 'category', width: 110 },
  { title: '标签', key: 'tags', width: 220 },
  { title: '数量', dataIndex: 'quantity', width: 70, align: 'right' },
  { title: '等级', dataIndex: 'level', width: 70, align: 'right' },
  { title: '价格', dataIndex: 'price', width: 90, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '来源', dataIndex: 'source', key: 'source', width: 100 },
  { title: '所属用户', dataIndex: 'user_id', width: 130 },
  { title: '建档时间', dataIndex: 'create_time', width: 170 },
];

export const searchFormSchema: FormSchema[] = [
  { label: '视图', field: 'view', component: 'Select', componentProps: { options: viewOptions, allowClear: false }, defaultValue: 'normal', colProps: { span: 6 } },
  { label: '物品ID', field: 'itemId', component: 'Input', colProps: { span: 6 } },
  { label: '关键字', field: 'keyword', component: 'Input', colProps: { span: 6 } },
  { label: '分类', field: 'category', component: 'Input', colProps: { span: 6 } },
  { label: '标签', field: 'tag', component: 'Input', colProps: { span: 6 } },
  { label: '等级', field: 'level', component: 'Input', colProps: { span: 6 } },
  { label: '状态', field: 'status', component: 'Select', componentProps: { options: statusOptions, allowClear: true }, colProps: { span: 6 } },
  { label: '来源', field: 'source', component: 'Select', componentProps: { options: sourceOptions, allowClear: true }, colProps: { span: 6 } },
  { label: '排序字段', field: 'orderBy', component: 'Select', componentProps: { options: orderByOptions, allowClear: true }, colProps: { span: 6 } },
  { label: '排序方向', field: 'orderDir', component: 'Select', componentProps: { options: orderDirOptions, allowClear: false }, defaultValue: 'desc', colProps: { span: 6 } },
  { label: '建档时间', field: 'createTime', component: 'RangePicker', colProps: { span: 6 } },
];

/**
 * 从 fields（JSON 字符串或组件数组）中解析标签：
 * element == 10 的组件 content.tags（content 可能为 JSON 字符串 / 对象 / 空格分隔字符串）
 */
export function parseItemTags(fields: any): string[] {
  if (!fields) return [];
  let arr: any[] = [];
  if (Array.isArray(fields)) {
    arr = fields;
  } else if (typeof fields === 'string') {
    try {
      arr = JSON.parse(fields) || [];
    } catch {
      return [];
    }
  }
  const tags: string[] = [];
  for (const f of arr) {
    if (String(f?.element) !== '10') continue;
    let val: any = f?.content;
    if (typeof val === 'string') {
      try {
        val = JSON.parse(val);
      } catch {
        // 保留原字符串
      }
    }
    if (Array.isArray(val?.tags)) {
      tags.push(...normalizeTagValues(val.tags));
    } else if (Array.isArray(val)) {
      tags.push(...normalizeTagValues(val));
    } else if (typeof val === 'string') {
      tags.push(...val.split(/\s+/).filter(Boolean));
    }
  }
  return [...new Set(tags)];
}

/** 标签值归一：元素可能是字符串或 {value|name|text|label} 槽位对象 */
function normalizeTagValues(list: any[]): string[] {
  return list
    .filter(Boolean)
    .map((t: any) => (typeof t === 'object' ? String(t?.value ?? t?.name ?? t?.text ?? t?.label ?? '') : String(t)).trim())
    .filter(Boolean);
}

/** 组件字段 content 展示格式化 */
export function formatFieldContent(content: any): string {
  if (content === null || content === undefined || content === '') return '-';
  if (typeof content === 'string') return content;
  if (typeof content === 'object') {
    try {
      return JSON.stringify(content);
    } catch {
      return String(content);
    }
  }
  return String(content);
}
