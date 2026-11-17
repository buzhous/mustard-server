import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';

export const columns: BasicColumn[] = [
  {
    title: '手机号',
    dataIndex: 'phone',
    width: 130,
  },
  {
    title: '昵称',
    dataIndex: 'nickname',
    width: 130,
  },
  {
    title: '仓库名称',
    dataIndex: 'name',
    width: 160,
    align: 'left',
  },
  {
    title: '仓库类型',
    dataIndex: 'storageType',
    width: 100,
    customRender: ({ text }) => (text === 'REAL' ? '自有真实仓' : '虚拟仓'),
  },
  {
    title: '默认仓',
    dataIndex: 'isDefault',
    width: 90,
    customRender: ({ text }) => {
      const isDefault = text == 1;
      return h(Tag, { color: isDefault ? 'blue' : 'default' }, () => (isDefault ? '默认' : '-'));
    },
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 160,
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    // 手机号/用户名/用户Id，后端解析后按用户过滤
    field: 'userKey',
    label: '用户',
    component: 'Input',
    colProps: { span: 6 },
  },
  {
    field: 'name',
    label: '仓库名称',
    component: 'Input',
    colProps: { span: 6 },
  },
];

export const bindColumns = [
  {
    title: '站点名称',
    dataIndex: 'stationName',
    width: 160,
  },
  {
    title: '站点编码',
    dataIndex: 'stationCode',
    width: 130,
  },
  {
    title: '自定义仓库名',
    dataIndex: 'customName',
    width: 140,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '主站点',
    dataIndex: 'isDefault',
    width: 90,
    customRender: ({ text }) => (text == 1 ? '是' : '否'),
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 90,
    customRender: ({ text }) => {
      const bound = text == 1;
      return h(Tag, { color: bound ? 'green' : 'default' }, () => (bound ? '已绑定' : '已解绑'));
    },
  },
  {
    title: '绑定时间',
    dataIndex: 'bindTime',
    width: 160,
  },
];
