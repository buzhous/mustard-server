import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';

/** 文章状态（与后端 Article 常量一致） */
export const ARTICLE_STATUS_OPTIONS = [
  { label: '已发布', value: 1, color: 'green' },
  { label: '已下架', value: 0, color: 'orange' },
];

export const articleStatusLabel = (value) =>
  ARTICLE_STATUS_OPTIONS.find((item) => item.value == value)?.label || '未知';

export const columns: BasicColumn[] = [
  {
    title: '文章标题',
    dataIndex: 'title',
    width: 200,
    align: 'left',
    ellipsis: true,
  },
  {
    title: '所属栏目',
    dataIndex: 'columnName',
    width: 100,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '文章编码',
    dataIndex: 'articleCode',
    width: 150,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '作者',
    dataIndex: 'author',
    width: 90,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '排序',
    dataIndex: 'sortNo',
    width: 70,
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 90,
    customRender: ({ text }) => {
      const option = ARTICLE_STATUS_OPTIONS.find((item) => item.value == text);
      return h(Tag, { color: option?.color || 'default' }, () => option?.label || '未知');
    },
  },
  {
    title: '发布时间',
    dataIndex: 'publishTime',
    width: 150,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    width: 150,
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'title',
    label: '文章标题',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: ARTICLE_STATUS_OPTIONS.map(({ label, value }) => ({ label, value })),
    },
    colProps: { span: 6 },
  },
];
