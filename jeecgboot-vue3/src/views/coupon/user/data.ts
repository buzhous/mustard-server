import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';
import { COUPON_TYPE_OPTIONS, valueText } from '/@/views/coupon/template/data';

/** 获取方式 */
export const OBTAIN_TYPE_OPTIONS = [
  { label: '主动领取', value: 1, color: 'blue' },
  { label: '系统发放', value: 2, color: 'purple' },
  { label: '活动奖励', value: 3, color: 'cyan' },
];

/** 持券状态 */
export const UC_STATUS_OPTIONS = [
  { label: '未使用', value: 0, color: 'green' },
  { label: '已使用', value: 1, color: 'default' },
  { label: '已过期', value: 2, color: 'gray' },
  { label: '已作废', value: 3, color: 'red' },
];

export const columns: BasicColumn[] = [
  {
    title: '用户',
    dataIndex: 'userName',
    width: 130,
    align: 'left',
    customRender: ({ text, record }) => {
      const label = text || record?.userId || '';
      return record?.userId ? `${label}(${record.userId})` : '-';
    },
  },
  {
    title: '券名称',
    dataIndex: 'couponName',
    width: 140,
  },
  {
    title: '类型',
    dataIndex: 'couponType',
    width: 80,
    customRender: ({ text }) => {
      const option = COUPON_TYPE_OPTIONS.find((item) => item.value == text);
      return h(Tag, { color: option?.color || 'default' }, () => option?.label || '未知');
    },
  },
  {
    title: '面额/门槛',
    dataIndex: 'discountValue',
    width: 130,
    customRender: ({ record }) => valueText(record),
  },
  {
    title: '获取方式',
    dataIndex: 'obtainType',
    width: 90,
    customRender: ({ text }) => {
      const option = OBTAIN_TYPE_OPTIONS.find((item) => item.value == text);
      return h(Tag, { color: option?.color || 'default' }, () => option?.label || '未知');
    },
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 80,
    customRender: ({ text }) => {
      const option = UC_STATUS_OPTIONS.find((item) => item.value == text);
      return h(Tag, { color: option?.color || 'default' }, () => option?.label || '未知');
    },
  },
  {
    title: '有效期至',
    dataIndex: 'validEndTime',
    width: 150,
  },
  {
    title: '使用时间',
    dataIndex: 'useTime',
    width: 150,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '获得时间',
    dataIndex: 'createTime',
    width: 150,
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'userKey',
    label: '用户',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    field: 'couponName',
    label: '券名称',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: UC_STATUS_OPTIONS.map(({ label, value }) => ({ label, value })),
      allowClear: true,
    },
    colProps: { span: 6 },
  },
  {
    field: 'obtainType',
    label: '获取方式',
    component: 'Select',
    componentProps: {
      options: OBTAIN_TYPE_OPTIONS.map(({ label, value }) => ({ label, value })),
      allowClear: true,
    },
    colProps: { span: 6 },
  },
];
