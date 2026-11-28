import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';

export const columns: BasicColumn[] = [
  {
    title: '排序',
    dataIndex: 'sort',
    width: 70,
  },
  {
    title: '图标',
    dataIndex: 'icon',
    width: 130,
  },
  {
    title: '分类名称',
    dataIndex: 'name',
    width: 140,
    align: 'left',
  },
  {
    title: '分类边界说明',
    dataIndex: 'remark',
    width: 380,
    ellipsis: true,
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 80,
    customRender: ({ text }) => {
      const enabled = text == 1;
      return h(Tag, { color: enabled ? 'green' : 'default' }, () => (enabled ? '启用' : '停用'));
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
    field: 'name',
    label: '分类名称',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: [
        { label: '启用', value: 1 },
        { label: '停用', value: 0 },
      ],
      allowClear: true,
    },
    colProps: { span: 6 },
  },
];

export const formSchema: FormSchema[] = [
  {
    label: '主键',
    field: 'id',
    component: 'Input',
    show: false,
  },
  {
    field: 'name',
    label: '分类名称',
    component: 'Input',
    required: true,
    helpMessage: '全局唯一（用户分类重名时以系统分类为准）',
  },
  {
    field: 'icon',
    label: '图标',
    component: 'Input',
    required: true,
    helpMessage: 'iconpark 图标名称，与 App 端图标渲染对应',
  },
  {
    field: 'sort',
    label: '排序',
    component: 'InputNumber',
    componentProps: { min: 0, precision: 0, style: { width: '180px' } },
    helpMessage: '越小越前，决定 App 系统分类展示顺序',
  },
  {
    field: 'status',
    label: '状态',
    component: 'RadioGroup',
    defaultValue: 1,
    helpMessage: '停用不物理删除：已实例化的用户分类（system_id 关联）保留聚合关系',
    componentProps: {
      options: [
        { label: '启用', value: 1 },
        { label: '停用', value: 0 },
      ],
    },
  },
  {
    field: 'remark',
    label: '分类边界说明',
    component: 'InputTextArea',
    colProps: { span: 24 },
    helpMessage: '运营维护，供 AI 建档识别与 App 展示参考',
    componentProps: {
      rows: 3,
    },
  },
];
