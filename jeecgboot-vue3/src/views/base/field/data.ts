import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';

/** 必填三要素：1 图标 / 2 名称 / 3 类别（与后端 BaseFieldServiceImpl REQUIRED_ELEMENTS 一致） */
export const REQUIRED_ELEMENTS = [1, 2, 3];

/** 输入类型（内置 11 种，可扩展；未知类型 App 按文本兜底渲染） */
export const INPUT_TYPE_OPTIONS = [
  '文本',
  '数字',
  '日期',
  '日期区间',
  '选择',
  '媒体',
  '文件',
  '组',
  '富文本',
  '定位',
  '模型',
];

const flagRender = ({ text }) =>
  h(Tag, { color: text == 1 ? 'green' : 'default' }, () => (text == 1 ? '已实现' : '未实现'));

export const columns: BasicColumn[] = [
  {
    title: '组件编号',
    dataIndex: 'element',
    width: 90,
    align: 'left',
  },
  {
    title: '字段名称',
    dataIndex: 'name',
    width: 130,
  },
  {
    title: '字段标识',
    dataIndex: 'fieldKey',
    width: 160,
  },
  {
    title: '输入类型',
    dataIndex: 'inputType',
    width: 100,
    customRender: ({ text }) => h(Tag, { color: 'blue' }, () => text || '未知'),
  },
  {
    title: '必填',
    dataIndex: 'isRequired',
    width: 70,
    customRender: ({ text }) => (text == 1 ? h(Tag, { color: 'red' }, () => '必填') : '—'),
  },
  {
    title: '默认排序',
    dataIndex: 'sort',
    width: 90,
  },
  {
    title: '安卓',
    dataIndex: 'androidFlag',
    width: 80,
    customRender: flagRender,
  },
  {
    title: 'iOS',
    dataIndex: 'iosFlag',
    width: 80,
    customRender: flagRender,
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
    field: 'element',
    label: '组件编号',
    component: 'InputNumber',
    componentProps: { min: 1, precision: 0 },
    colProps: { span: 6 },
  },
  {
    field: 'name',
    label: '字段名称',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    field: 'inputType',
    label: '输入类型',
    component: 'Select',
    componentProps: {
      options: INPUT_TYPE_OPTIONS.map((value) => ({ label: value, value })),
      allowClear: true,
    },
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
  {
    field: 'androidFlag',
    label: '安卓',
    component: 'Select',
    componentProps: {
      options: [
        { label: '已实现', value: 1 },
        { label: '未实现', value: 0 },
      ],
      allowClear: true,
    },
    colProps: { span: 6 },
  },
  {
    field: 'iosFlag',
    label: 'iOS',
    component: 'Select',
    componentProps: {
      options: [
        { label: '已实现', value: 1 },
        { label: '未实现', value: 0 },
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
    field: 'element',
    label: '组件编号',
    component: 'InputNumber',
    componentProps: { min: 1, precision: 0, style: { width: '180px' } },
    helpMessage: '全局唯一；内置 1-34，扩展新增可留空由后端自动顺延分配（35 起）',
  },
  {
    field: 'name',
    label: '字段名称',
    component: 'Input',
    required: true,
  },
  {
    field: 'fieldKey',
    label: '字段标识',
    component: 'Input',
    required: true,
    helpMessage: '英文驼峰，与物品 fields JSON 的 key 对应',
  },
  {
    field: 'inputType',
    label: '输入类型',
    component: 'AutoComplete',
    required: true,
    helpMessage: '内置 11 种，可扩展自定义类型；未知类型 App 按文本兜底渲染',
    componentProps: {
      options: INPUT_TYPE_OPTIONS.map((value) => ({ value })),
      filterOption: (input: string, option: Recordable) =>
        String(option?.value ?? '').toLowerCase().includes(String(input ?? '').toLowerCase()),
      placeholder: '选择或输入自定义输入类型',
    },
  },
  {
    field: 'isRequired',
    label: '是否必填',
    component: 'RadioGroup',
    defaultValue: 0,
    componentProps: {
      options: [
        { label: '否', value: 0 },
        { label: '是', value: 1 },
      ],
    },
  },
  {
    field: 'sort',
    label: '默认排序',
    component: 'InputNumber',
    componentProps: { min: 0, precision: 0, style: { width: '180px' } },
    helpMessage: '决定 App 表单/详情的展示顺序；留空时后端按组件编号落定（扩展字段排末尾）',
  },
  {
    field: 'platformFlags',
    label: '端实现标记',
    component: 'CheckboxGroup',
    defaultValue: ['android', 'ios'],
    helpMessage: '勾选 = 该端已实现此字段渲染；未勾选端 App 初始化不会加载该字段',
    componentProps: {
      options: [
        { label: '安卓', value: 'android' },
        { label: 'iOS', value: 'ios' },
      ],
    },
  },
  {
    field: 'status',
    label: '状态',
    component: 'RadioGroup',
    defaultValue: 1,
    componentProps: {
      options: [
        { label: '启用', value: 1 },
        { label: '停用', value: 0 },
      ],
    },
  },
  {
    field: 'remark',
    label: '备注',
    component: 'InputTextArea',
    colProps: { span: 24 },
    componentProps: {
      rows: 3,
    },
  },
];
