import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';
import { columnList } from '/@/api/article/articleAdminApi';

/** 文章状态（与后端 Article 常量一致） */
export const ARTICLE_STATUS_OPTIONS = [
  { label: '已发布', value: 1, color: 'green' },
  { label: '已下架', value: 0, color: 'orange' },
];

/** 栏目下拉选项（搜索过滤用；返回 label/value 供 ApiSelect 直接使用） */
export const getArticleColumnOptions = () =>
  columnList().then((records: any[]) =>
    (records || []).map((c) => ({ label: `${c.columnName}（${c.columnCode}）`, value: c.id }))
  );

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
    width: 180,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    width: 180,
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
    field: 'columnId',
    label: '所属栏目',
    component: 'ApiSelect',
    componentProps: {
      api: getArticleColumnOptions,
      placeholder: '请选择所属栏目',
      allowClear: true,
    },
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
