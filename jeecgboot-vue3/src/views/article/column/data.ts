import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn } from '/@/components/Table';

/** 栏目状态 */
export const COLUMN_STATUS_OPTIONS = [
  { label: '启用', value: 1, color: 'green' },
  { label: '停用', value: 0, color: 'orange' },
];

export const columns: BasicColumn[] = [
  {
    title: '栏目名称',
    dataIndex: 'columnName',
    width: 160,
  },
  {
    title: '栏目编码',
    dataIndex: 'columnCode',
    width: 160,
  },
  {
    title: '排序',
    dataIndex: 'sortNo',
    width: 80,
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 90,
    customRender: ({ text }) => {
      const option = COLUMN_STATUS_OPTIONS.find((item) => item.value == text);
      return h(Tag, { color: option?.color || 'default' }, () => option?.label || '未知');
    },
  },
  {
    title: '备注',
    dataIndex: 'remark',
    width: 220,
    align: 'left',
    ellipsis: true,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 150,
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    width: 150,
  },
];
