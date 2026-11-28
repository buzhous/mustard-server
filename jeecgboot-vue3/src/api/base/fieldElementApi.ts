import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/admin/fieldElement/list',
  queryById = '/admin/fieldElement/queryById',
  add = '/admin/fieldElement/add',
  edit = '/admin/fieldElement/edit',
  status = '/admin/fieldElement/status',
}

/**
 * 字段字典分页列表（读 Redis 缓存，按 sort 升序）
 * @param params 支持 element/name/inputType/androidFlag/iosFlag/status 筛选
 */
export const list = (params) => {
  return defHttp.get({ url: Api.list, params });
};

/**
 * 字段详情（抽屉编辑回填）
 * @param params
 */
export const queryById = (params) => {
  return defHttp.get({ url: Api.queryById, params });
};

/**
 * 新增字段（组件编号可留空，后端自动顺延分配 35+）
 * @param params
 */
export const add = (params) => {
  return defHttp.post({ url: Api.add, params });
};

/**
 * 编辑字段（组件编号锁定，后端强制以库内编号为准）
 * @param params
 */
export const edit = (params) => {
  return defHttp.put({ url: Api.edit, params });
};

/**
 * 启用/停用字段（已录入字段不可删除，仅支持启停）
 * @param params
 * @param handleSuccess
 */
export const changeStatus = (params, handleSuccess) => {
  return defHttp.post({ url: Api.status, params }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};

/** 字典选项（FieldConfigEditor 等下游组件使用） */
export interface FieldElementOption {
  element: number;
  name: string;
  fieldKey: string;
  inputType: string;
  status: number;
  sort: number;
}

/**
 * 字段字典全量选项（读缓存列表接口，pageSize 上限 200，超出自动翻页补齐）。
 * 返回按 sort 升序的全量字典（含停用项，供已引用字段回显）；调用方自行过滤启用项。
 */
export const getFieldElementOptions = async (): Promise<FieldElementOption[]> => {
  const all: FieldElementOption[] = [];
  const pageSize = 200;
  for (let pageNo = 1; pageNo <= 20; pageNo++) {
    const res: any = await defHttp.get(
      { url: Api.list, params: { pageNo, pageSize } },
      { successMessageMode: 'none' }
    );
    const records = (res?.records ?? []) as FieldElementOption[];
    all.push(...records);
    const total = Number(res?.total ?? 0);
    if (all.length >= total || records.length === 0) {
      break;
    }
  }
  return all;
};
