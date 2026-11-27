<template>
  <div>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" v-auth="'ailing:rebuild'" :loading="polling" preIcon="ant-design:sync-outlined" @click="handleRebuild">重建检索索引</a-button>
        <span v-if="rebuildText" style="margin-left: 12px; color: #999">{{ rebuildText }}</span>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <a @click="openDetail(record)" style="display: inline-flex; align-items: center; gap: 4px">
            <a-avatar v-if="record.icon" :src="record.icon" :size="22" shape="square" />
            {{ record.name || record.item_id || record.id }}
          </a>
        </template>
        <template v-else-if="column.key === 'tags'">
          <template v-if="parseItemTags(record.fields).length">
            <a-tag v-for="t in parseItemTags(record.fields)" :key="t" color="blue" style="margin-inline-end: 4px">{{ t }}</a-tag>
          </template>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag v-if="record.status != null" :color="statusColorMap[String(record.status)] || 'default'">
            {{ statusMap[String(record.status)] ?? record.status }}
          </a-tag>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'source'">
          <a-tag v-if="record.source" :color="sourceColorMap[record.source] || 'default'">{{ sourceMap[record.source] || record.source }}</a-tag>
          <span v-else>-</span>
        </template>
      </template>
    </BasicTable>
    <ItemDetailDrawer @register="registerDrawer" />
  </div>
</template>

<script lang="ts" name="ailing-items" setup>
  import { onMounted, onUnmounted, ref } from 'vue';
  import { useRoute } from 'vue-router';
  import dayjs from 'dayjs';
  import { BasicTable, useTable } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useMessage } from '/@/hooks/web/useMessage';
  import ItemDetailDrawer from './ItemDetailDrawer.vue';
  import { columns, searchFormSchema, parseItemTags, statusMap, statusColorMap, sourceMap, sourceColorMap } from './data';
  import { getItemList, getSearchIndexProgress, rebuildSearchIndex } from './api';

  const route = useRoute();
  const { createMessage } = useMessage();
  const [registerDrawer, { openDrawer }] = useDrawer();

  const [registerTable, { reload, getForm }] = useTable({
    title: '芥灵物品',
    api: getItemList,
    rowKey: 'id',
    columns,
    canResize: false,
    showTableSetting: true,
    formConfig: {
      schemas: searchFormSchema,
      autoSubmitOnEnter: true,
      fieldMapToTime: [['createTime', ['createTimeStart', 'createTimeEnd'], 'YYYY-MM-DD']],
    },
  });

  // 支持 stats 页钻取：/ailing/items?category=xxx&tag=xxx&level=x&source=xxx&createTimeStart=&createTimeEnd=
  onMounted(async () => {
    const q: Recordable = {};
    for (const k of ['view', 'itemId', 'keyword', 'category', 'tag', 'level', 'status', 'source', 'orderBy', 'orderDir']) {
      const v = route.query[k];
      if (v !== undefined && v !== '') {
        q[k] = k === 'status' ? Number(v) : v;
      }
    }
    if (route.query.createTimeStart && route.query.createTimeEnd) {
      q.createTime = [dayjs(String(route.query.createTimeStart)), dayjs(String(route.query.createTimeEnd))];
    }
    if (!Object.keys(q).length) return;
    try {
      await getForm().setFieldsValue(q);
    } catch {
      // 表单未就绪时忽略
    }
    await reload({ page: 1 });
  });

  function openDetail(record: Recordable) {
    openDrawer(true, { record });
  }

  // ---------- 重建检索索引 + 进度轮询 ----------
  const polling = ref(false);
  const rebuildText = ref('');
  let rebuildTimer: ReturnType<typeof setInterval> | null = null;

  function stopPolling() {
    if (rebuildTimer) {
      clearInterval(rebuildTimer);
      rebuildTimer = null;
    }
    polling.value = false;
  }

  async function handleRebuild() {
    if (polling.value) return;
    await rebuildSearchIndex().catch(() => null);
    polling.value = true;
    rebuildText.value = '重建进行中…';
    rebuildTimer = setInterval(pollProgress, 1500);
  }

  async function pollProgress() {
    try {
      const p: any = await getSearchIndexProgress();
      if (p?.running) {
        rebuildText.value = `重建进度：${p.done ?? 0}/${p.total ?? 0}${p.fail ? `，失败 ${p.fail}` : ''}`;
        return;
      }
      stopPolling();
      rebuildText.value = '';
      createMessage.success(`检索索引重建完成：成功 ${p?.done ?? 0}${p?.fail ? `，失败 ${p?.fail}` : ''}`);
      reload();
    } catch {
      // 进度查询异常则停止轮询，避免死循环
      stopPolling();
      rebuildText.value = '';
    }
  }

  onUnmounted(stopPolling);
</script>
