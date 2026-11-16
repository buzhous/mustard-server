import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';

/** 反馈状态（与后端 Feedback 常量一致） */
export const FEEDBACK_STATUS_OPTIONS = [
  { label: '待处理', value: 0, color: 'orange' },
  { label: '已处理', value: 1, color: 'green' },
];

export const feedbackStatusLabel = (value) =>
  FEEDBACK_STATUS_OPTIONS.find((item) => item.value == value)?.label || '未知';

export const columns: BasicColumn[] = [
  {
    title: '反馈内容',
    dataIndex: 'content',
    width: 220,
    align: 'left',
    ellipsis: true,
  },
  {
    title: '用户昵称',
    dataIndex: 'nickname',
    width: 110,
  },
  {
    title: '手机号',
    dataIndex: 'phone',
    width: 120,
  },
  {
    title: '联系方式',
    dataIndex: 'contact',
    width: 120,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 90,
    customRender: ({ text }) => {
      const option = FEEDBACK_STATUS_OPTIONS.find((item) => item.value == text);
      return h(Tag, { color: option?.color || 'default' }, () => option?.label || '未知');
    },
  },
  {
    title: '运营回复',
    dataIndex: 'reply',
    width: 200,
    align: 'left',
    ellipsis: true,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '提交时间',
    dataIndex: 'createTime',
    width: 150,
  },
  {
    title: '处理时间',
    dataIndex: 'updateTime',
    width: 150,
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: FEEDBACK_STATUS_OPTIONS.map(({ label, value }) => ({ label, value })),
    },
    colProps: { span: 6 },
  },
  {
    field: 'content',
    label: '内容关键字',
    component: 'JInput',
    colProps: { span: 6 },
  },
];
