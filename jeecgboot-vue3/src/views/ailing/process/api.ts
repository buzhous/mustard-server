import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/admin/ailing/session/list',
  detail = '/admin/ailing/session/detail',
  stats = '/admin/ailing/session/stats',
  auditList = '/admin/ailing/session/audit/list',
}

/**
 * 会话列表
 * 服务端返回 {records, pageNo, pageSize}，无 total，前端按「下一页是否存在」本地估算 total（P2 items 先例）
 */
export function getSessionList(params?: Recordable) {
  return defHttp
    .get({ url: Api.list, params }, { successMessageMode: 'none' })
    .then((res: any) => {
      const records = res?.records ?? [];
      const pageNo = res?.pageNo ?? params?.pageNo ?? 1;
      const pageSize = res?.pageSize ?? params?.pageSize ?? 10;
      const base = (pageNo - 1) * pageSize;
      const total = records.length < pageSize ? base + records.length : base + pageSize + 1;
      return { records, total };
    });
}

/** 会话详情回放：{session, messages}（需要 ailing:session:view 权限，读取即审计） */
export const getSessionDetail = (params: Recordable) => defHttp.get({ url: Api.detail, params }, { successMessageMode: 'none' });

/** 质量看板：{summary, askDist, sessionTrend, modeDist} */
export const getSessionStats = () => defHttp.get({ url: Api.stats }, { successMessageMode: 'none' });

/** 会话回放查看审计（最近 200 条） */
export const getSessionAuditList = () => defHttp.get({ url: Api.auditList }, { successMessageMode: 'none' });
