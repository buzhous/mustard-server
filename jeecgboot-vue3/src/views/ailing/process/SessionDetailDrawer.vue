<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" :title="drawerTitle" width="720" destroyOnClose>
    <a-spin :spinning="detailLoading">
      <a-descriptions bordered size="small" :column="2" class="mb-2">
        <a-descriptions-item label="会话ID">{{ session?.id ?? '-' }}</a-descriptions-item>
        <a-descriptions-item label="用户">{{ session?.username || session?.uid || '-' }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag v-if="session?.status" :color="sessionStatusColorMap[session.status] || 'default'">{{ sessionStatusMap[session.status] || session.status }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ session?.create_time ?? '-' }}</a-descriptions-item>
      </a-descriptions>
      <SessionReplay :messages="messages" @goto-item="gotoItem" />

      <a-divider>查看审计（最近{{ auditList.length }}条）</a-divider>
      <div v-for="(a, i) in auditList" :key="i" style="font-size: 12px; color: #999; margin-bottom: 2px">
        {{ a.uid || '-' }} 于 {{ a.server_ts ? new Date(Number(a.server_ts)).toLocaleString() : '-' }} 查看本会话
      </div>
      <a-empty v-if="!auditList.length" description="暂无查看记录" />
    </a-spin>
  </BasicDrawer>
</template>

<script lang="ts" name="ailing-session-detail-drawer" setup>
  import { computed, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import SessionReplay from './SessionReplay.vue';
  import { sessionStatusMap, sessionStatusColorMap } from './data';
  import { getSessionAuditList, getSessionDetail } from './api';

  const router = useRouter();
  const detailLoading = ref(false);
  const session = ref<Recordable>({});
  const messages = ref<Recordable[]>([]);
  const auditList = ref<Recordable[]>([]);
  const drawerTitle = computed(() => `会话回放${session.value?.title ? ` · ${session.value.title}` : ''}`);

  // itemId → 物品列表页钻取（items 页支持 itemId 查询参数）
  function gotoItem(itemId: string) {
    router.push({ path: '/ailing/items', query: { itemId } });
  }

  const [registerDrawer] = useDrawerInner(async (data) => {
    const record = data?.record || {};
    detailLoading.value = true;
    session.value = {};
    messages.value = [];
    auditList.value = [];
    try {
      const d: any = await getSessionDetail({ sessionId: record.id });
      session.value = d?.session || {};
      messages.value = d?.messages || [];
      const a: any = await getSessionAuditList().catch(() => null);
      auditList.value = (a || []).filter((x: any) => {
        try {
          const props = typeof x.props === 'string' ? JSON.parse(x.props) : x.props;
          return props?.sessionId === record.id;
        } catch {
          return false;
        }
      });
    } catch {
      session.value = {};
      messages.value = [];
    } finally {
      detailLoading.value = false;
    }
  });
</script>
