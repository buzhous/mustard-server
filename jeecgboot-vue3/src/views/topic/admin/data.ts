import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';
import { getSystemCategoryOptions } from '/@/api/topic/topicAdminApi';

/** 系统分类下拉搜索过滤：分类名称 / 边界说明 remark / id 任一命中 */
export function filterCateOption(input: string, option: Recordable) {
  const key = String(input ?? '').trim().toLowerCase();
  if (!key) return true;
  return (
    String(option?.label ?? '').toLowerCase().includes(key) ||
    String(option?.remark ?? '').toLowerCase().includes(key) ||
    String(option?.value ?? '').includes(key)
  );
}

/** 主题类型文案（与后端 TopicFieldClassifier 推导规则一致） */
export const TOPIC_TYPE_OPTIONS = [
  { label: '基础型', value: 1, color: 'blue' },
  { label: '多媒体型', value: 2, color: 'green' },
  { label: '模型型', value: 3, color: 'purple' },
  { label: '扩展型', value: 4, color: 'orange' },
];

export const topicTypeLabel = (value) => TOPIC_TYPE_OPTIONS.find((item) => item.value == value)?.label || '未知';

export const columns: BasicColumn[] = [
  {
    title: '主题名称',
    dataIndex: 'topicName',
    width: 160,
    align: 'left',
  },
  {
    title: '子标题',
    dataIndex: 'topicSubtitle',
    width: 180,
  },
  {
    title: '主题类型',
    dataIndex: 'topicType',
    width: 100,
    customRender: ({ text }) => {
      const option = TOPIC_TYPE_OPTIONS.find((item) => item.value == text);
      return h(Tag, { color: option?.color || 'default' }, () => option?.label || '未知');
    },
  },
  {
    title: '热门',
    dataIndex: 'isHot',
    width: 70,
    customRender: ({ text }) => (text == 1 ? '是' : '否'),
  },
  {
    title: '状态',
    dataIndex: 'topicStatus',
    width: 80,
    customRender: ({ text }) => {
      const enabled = text == 0;
      return h(Tag, { color: enabled ? 'green' : 'default' }, () => (enabled ? '启用' : '停用'));
    },
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

export const searchFormSchema: FormSchema[] = [
  {
    field: 'topicName',
    label: '主题名称',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    field: 'topicType',
    label: '主题类型',
    component: 'Select',
    componentProps: {
      options: TOPIC_TYPE_OPTIONS.map(({ label, value }) => ({ label, value })),
    },
    colProps: { span: 6 },
  },
  {
    field: 'topicStatus',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: [
        { label: '启用', value: 0 },
        { label: '停用', value: 1 },
      ],
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
    field: 'topicName',
    label: '主题名称',
    component: 'Input',
    required: true,
  },
  {
    field: 'topicSubtitle',
    label: '子标题',
    component: 'Input',
    helpMessage: '展示在添加列表主题卡片标题下方',
  },
  {
    field: 'topicDesc',
    label: '主题描述',
    component: 'InputTextArea',
    colProps: { span: 24 },
    componentProps: {
      rows: 3,
    },
  },
  {
    field: 'topicIcon',
    label: '主题图标',
    component: 'JImageUpload',
    componentProps: {
      fileMax: 1,
    },
  },
  {
    field: 'bgImage',
    label: '背景图',
    component: 'JImageUpload',
    componentProps: {
      fileMax: 1,
    },
    helpMessage: '展示在添加列表主题卡片背景，建议竖版图片',
  },
  {
    field: 'topicCate',
    label: '主题分类',
    component: 'ApiSelect',
    required: true,
    helpMessage: '采用系统分类（C3 分类治理），支持名称/说明/ID 搜索过滤；停用分类置灰',
    componentProps: {
      api: getSystemCategoryOptions,
      labelField: 'name',
      valueField: 'id',
      placeholder: '输入分类名称/说明搜索系统分类',
      allowClear: true,
      filterOption: filterCateOption,
    },
  },
  {
    field: 'itemCate',
    label: '物品分类',
    component: 'ApiSelect',
    helpMessage: 'App 建档默认分类（系统分类，如证件主题→证件票据）；为空时 App 按主题默认规则落定',
    componentProps: {
      api: getSystemCategoryOptions,
      labelField: 'name',
      valueField: 'id',
      placeholder: '输入分类名称/说明搜索系统分类',
      allowClear: true,
      filterOption: filterCateOption,
    },
  },
  {
    field: 'isHot',
    label: '是否热门',
    component: 'Select',
    defaultValue: 1,
    componentProps: {
      options: [
        { label: '是', value: 1 },
        { label: '否', value: 0 },
      ],
    },
  },
  {
    field: 'isCharge',
    label: '是否收费',
    component: 'Select',
    defaultValue: 0,
    componentProps: {
      options: [
        { label: '免费', value: 0 },
        { label: '收费', value: 1 },
      ],
    },
  },
  {
    field: 'price',
    label: '主题价格',
    component: 'InputNumber',
    componentProps: {
      min: 0,
      precision: 2,
    },
    ifShow: ({ values }) => values.isCharge == 1,
  },
  {
    field: 'topicStatus',
    label: '状态',
    component: 'Select',
    defaultValue: 0,
    componentProps: {
      options: [
        { label: '启用', value: 0 },
        { label: '停用', value: 1 },
      ],
    },
  },
];
