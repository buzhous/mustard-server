import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';

/** 券类型（与后端一致：1满减 2折扣 3兑换） */
export const COUPON_TYPE_OPTIONS = [
  { label: '满减券', value: 1, color: 'green' },
  { label: '折扣券', value: 2, color: 'blue' },
  { label: '兑换券', value: 3, color: 'purple' },
];

/** 券状态 */
export const COUPON_STATUS_OPTIONS = [
  { label: '未发布', value: 0, color: 'default' },
  { label: '已发布', value: 1, color: 'green' },
  { label: '已下架', value: 2, color: 'orange' },
];

export const typeLabel = (value) => COUPON_TYPE_OPTIONS.find((item) => item.value == value)?.label || '未知';
export const statusLabel = (value) => COUPON_STATUS_OPTIONS.find((item) => item.value == value)?.label || '未知';

/** 面额展示 */
export function valueText(record) {
  if (record.couponType == 1) {
    return `减${record.discountValue} 满${record.minAmount || 0}`;
  }
  if (record.couponType == 2) {
    // 折扣口径兼容：0~1 为原价比例（0.95=九五折），1~10 为折扣数（8.5=8.5折）
    const v = Number(record.discountValue);
    if (v > 0 && v <= 1) {
      return `${Math.round(v * 100) / 10}折`;
    }
    return `${v}折`;
  }
  return record.discountValue != null ? String(record.discountValue) : '-';
}

export const columns: BasicColumn[] = [
  {
    title: '券名称',
    dataIndex: 'couponName',
    width: 150,
    align: 'left',
  },
  {
    title: '券编码',
    dataIndex: 'code',
    width: 130,
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
    title: '总库存',
    dataIndex: 'totalCount',
    width: 90,
  },
  {
    title: '剩余',
    dataIndex: 'remainCount',
    width: 90,
  },
  {
    title: '已使用',
    dataIndex: 'usedCount',
    width: 80,
  },
  {
    title: '每人限领',
    dataIndex: 'perLimit',
    width: 90,
    customRender: ({ text }) => (text > 0 ? String(text) : '不限'),
  },
  {
    title: '每日限领',
    dataIndex: 'dailyLimit',
    width: 90,
    customRender: ({ text }) => (text > 0 ? String(text) : '不限'),
  },
  {
    title: '有效期',
    dataIndex: 'validStartTime',
    width: 280,
    customRender: ({ record }) => {
      const start = (record.validStartTime || '').replace(' ', '');
      const end = (record.validEndTime || '').replace(' ', '');
      return start && end ? `${record.validStartTime} ~ ${record.validEndTime}` : '-';
    },
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 80,
    customRender: ({ text }) => {
      const option = COUPON_STATUS_OPTIONS.find((item) => item.value == text);
      return h(Tag, { color: option?.color || 'default' }, () => option?.label || '未知');
    },
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    width: 150,
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'couponName',
    label: '券名称',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    field: 'couponType',
    label: '类型',
    component: 'Select',
    componentProps: {
      options: COUPON_TYPE_OPTIONS.map(({ label, value }) => ({ label, value })),
      allowClear: true,
    },
    colProps: { span: 6 },
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: COUPON_STATUS_OPTIONS.map(({ label, value }) => ({ label, value })),
      allowClear: true,
    },
    colProps: { span: 6 },
  },
];
