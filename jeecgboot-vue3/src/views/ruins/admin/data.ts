import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';

/** 殷墟状态（与后端 RuinsStatusEnum 一致） */
export const RUINS_STATUS_OPTIONS = [
  { label: '待拾取', value: 0, color: 'green' },
  { label: '已拾取', value: 1, color: 'default' },
  { label: '已收回', value: 2, color: 'orange' },
  { label: '已充公', value: 3, color: 'red' },
  { label: '已下架', value: 4, color: 'gray' },
];

export const ruinsStatusLabel = (value) => RUINS_STATUS_OPTIONS.find((item) => item.value == value)?.label || '未知';

export const columns: BasicColumn[] = [
  {
    title: '物品名称',
    dataIndex: 'name',
    width: 160,
    align: 'left',
  },
  {
    title: '物品编码',
    dataIndex: 'itemId',
    width: 160,
  },
  {
    title: '分类',
    dataIndex: 'categoryId',
    width: 100,
  },
  {
    title: '等级',
    dataIndex: 'level',
    width: 70,
  },
  {
    title: '数量',
    dataIndex: 'quantity',
    width: 70,
  },
  {
    title: '价格',
    dataIndex: 'price',
    width: 90,
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 90,
    customRender: ({ text }) => {
      const option = RUINS_STATUS_OPTIONS.find((item) => item.value == text);
      return h(Tag, { color: option?.color || 'default' }, () => option?.label || '未知');
    },
  },
  {
    title: '指定拾取者',
    dataIndex: 'pickupUserName',
    width: 120,
    customRender: ({ text, record }) => {
      const label = text || record?.pickupUser || '';
      return label || '-';
    },
  },
  {
    title: '拾取时间',
    dataIndex: 'pickupTime',
    width: 150,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 150,
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '物品名称',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: RUINS_STATUS_OPTIONS.map(({ label, value }) => ({ label, value })),
    },
    colProps: { span: 6 },
  },
];
