import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';
import { getAreaTextByCode } from '/@/components/Form/src/utils/Area';

// 由区县编码转换完整区域名称（如 410505 -> 河南省/安阳市/殷都区）
const renderAreaText = ({ record }) => (record.district ? getAreaTextByCode(record.district) : '') || '-';

export const columns: BasicColumn[] = [
  {
    title: '站点名称',
    dataIndex: 'stationName',
    width: 160,
    align: 'left',
  },
  {
    title: '站点编码',
    dataIndex: 'stationCode',
    width: 130,
  },
  {
    title: '所在区域',
    dataIndex: 'province',
    width: 200,
    customRender: renderAreaText,
  },
  {
    title: '详细地址',
    dataIndex: 'address',
    width: 180,
  },
  {
    title: '可存品类',
    dataIndex: 'storageCategories',
    width: 160,
  },
  {
    title: '库存总容量',
    dataIndex: 'totalCapacity',
    width: 100,
  },
  {
    title: '已用容量',
    dataIndex: 'usedCapacity',
    width: 90,
  },
  {
    title: '剩余容量',
    dataIndex: 'remainCapacity',
    width: 90,
    customRender: ({ record }) => {
      const total = Number(record.totalCapacity || 0);
      const used = Number(record.usedCapacity || 0);
      return total - used;
    },
  },
  {
    title: '营业时间',
    dataIndex: 'businessHours',
    width: 120,
  },
  {
    title: '联系电话',
    dataIndex: 'contactPhone',
    width: 130,
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 80,
    customRender: ({ text }) => {
      const isOpen = text == 1;
      return h(Tag, { color: isOpen ? 'green' : 'default' }, () => (isOpen ? '开放' : '停用'));
    },
  },
  {
    title: '排序',
    dataIndex: 'sort',
    width: 70,
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    width: 150,
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'stationName',
    label: '站点名称',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    // 只选到市级，值为城市行政区域编码（如 410500），与后端 city 字段精确匹配
    field: 'city',
    label: '城市',
    component: 'JAreaSelect',
    componentProps: { level: 2 },
    colProps: { span: 6 },
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: [
        { label: '开放', value: 1 },
        { label: '停用', value: 0 },
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
    field: 'stationCode',
    label: '站点编码',
    component: 'Input',
    required: true,
  },
  {
    field: 'stationName',
    label: '站点名称',
    component: 'Input',
    required: true,
  },
  {
    // 省市区三联下拉，值为区县行政区域编码（如 410505），编辑时组件自动反推省市回显
    field: 'district',
    label: '所在区域',
    component: 'JAreaSelect',
    required: true,
    componentProps: { level: 3, placeholder: '请选择省市区' },
  },
  {
    field: 'address',
    label: '详细地址',
    component: 'Input',
  },
  {
    field: 'longitude',
    label: '经度',
    component: 'InputNumber',
  },
  {
    field: 'latitude',
    label: '纬度',
    component: 'InputNumber',
  },
  {
    field: 'coverArea',
    label: '覆盖区域',
    component: 'Input',
    helpMessage: '覆盖城市/区域，多个用逗号分隔',
  },
  {
    field: 'storageCategories',
    label: '可存品类',
    component: 'Input',
    helpMessage: '可存储品类，多个用逗号分隔',
  },
  {
    field: 'warehouseStandard',
    label: '仓储标准',
    component: 'InputTextArea',
  },
  {
    field: 'feeStandard',
    label: '收费标准',
    component: 'InputTextArea',
    helpMessage: 'JSON 文本，如按时长/按体积计费',
  },
  {
    field: 'deliveryScope',
    label: '配送覆盖范围',
    component: 'Input',
  },
  {
    field: 'pickupScope',
    label: '上门取件范围',
    component: 'Input',
  },
  {
    field: 'estimateDays',
    label: '预计时效(天)',
    component: 'InputNumber',
    componentProps: {
      min: 0,
      precision: 0,
    },
  },
  {
    field: 'totalCapacity',
    label: '库存总容量',
    component: 'InputNumber',
    componentProps: {
      min: 0,
      precision: 0,
    },
  },
  {
    field: 'businessHours',
    label: '营业时间',
    component: 'Input',
  },
  {
    field: 'contactPhone',
    label: '联系电话',
    component: 'Input',
  },
  {
    field: 'stationImage',
    label: '站点图片',
    component: 'JImageUpload',
    componentProps: {
      fileMax: 1,
    },
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    defaultValue: 1,
    componentProps: {
      options: [
        { label: '开放', value: 1 },
        { label: '停用', value: 0 },
      ],
    },
  },
  {
    field: 'sort',
    label: '排序',
    component: 'InputNumber',
    defaultValue: 0,
    componentProps: {
      precision: 0,
    },
  },
];
