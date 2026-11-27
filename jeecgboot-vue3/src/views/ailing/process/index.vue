<template>
  <div class="ailing-process" style="padding: 16px">
    <!-- 质量看板区 -->
    <a-spin :spinning="statsLoading">
      <a-row :gutter="16">
        <a-col :span="6">
          <a-card size="small"><a-statistic title="建档成功率" :value="summaryCards.filedRate" suffix="%" /></a-card>
        </a-col>
        <a-col :span="6">
          <a-card size="small"><a-statistic title="express 直达率" :value="summaryCards.expressDirectRate" suffix="%" /></a-card>
        </a-col>
        <a-col :span="6">
          <a-card size="small"><a-statistic title="平均置信度" :value="summaryCards.avgConfidence" /></a-card>
        </a-col>
        <a-col :span="6">
          <a-card size="small"><a-statistic title="会话总数" :value="summaryCards.totalSessions ?? '-'" /></a-card>
        </a-col>
      </a-row>
      <a-row :gutter="16" style="margin-top: 16px">
        <a-col :span="8">
          <a-card size="small" title="追问轮次分布">
            <div ref="askRef" style="height: 260px; width: 100%"></div>
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card size="small" title="会话量趋势（近30天）">
            <div ref="trendRef" style="height: 260px; width: 100%"></div>
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card size="small" title="模式分布">
            <div ref="modeRef" style="height: 260px; width: 100%"></div>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>

    <!-- 会话列表 -->
    <BasicTable @register="registerTable" style="margin-top: 16px">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'title'">
          <a @click="openDetail(record)">{{ record.title || '（无标题）' }}</a>
        </template>
        <template v-else-if="column.key === 'mode'">
          <a-tag v-if="record.mode" :color="modeColorMap[record.mode] || 'default'">{{ modeMap[record.mode] || record.mode }}</a-tag>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag v-if="record.status" :color="sessionStatusColorMap[record.status] || 'default'">{{ sessionStatusMap[record.status] || record.status }}</a-tag>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'last_confidence'">
          <span v-if="record.last_confidence != null">{{ record.last_confidence }}</span>
          <span v-else>-</span>
        </template>
      </template>
    </BasicTable>

    <!-- 会话详情抽屉（回放 + 审计） -->
    <SessionDetailDrawer @register="registerDrawer" />
  </div>
</template>

<script lang="ts" name="ailing-process" setup>
  import { Ref, computed, onMounted, ref } from 'vue';
  import { useRoute } from 'vue-router';
  import { BasicTable, useTable } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useECharts } from '/@/hooks/web/useECharts';
  import SessionDetailDrawer from './SessionDetailDrawer.vue';
  import { columns, searchFormSchema, modeMap, modeColorMap, sessionStatusMap, sessionStatusColorMap } from './data';
  import { getSessionList, getSessionStats } from './api';

  const route = useRoute();

  // ---------- 质量看板 ----------
  const statsLoading = ref(false);
  const stats = ref<Recordable>({});
  const askRef = ref<HTMLDivElement | null>(null);
  const trendRef = ref<HTMLDivElement | null>(null);
  const modeRef = ref<HTMLDivElement | null>(null);
  const askChart = useECharts(askRef as Ref<HTMLDivElement>);
  const trendChart = useECharts(trendRef as Ref<HTMLDivElement>);
  const modeChart = useECharts(modeRef as Ref<HTMLDivElement>);

  const summary = computed<Recordable>(() => stats.value?.summary || {});
  const summaryCards = computed(() => ({
    filedRate: summary.value.filedRate ?? 0,
    expressDirectRate: summary.value.expressDirectRate ?? 0,
    avgConfidence: summary.value.avgConfidence ?? '-',
    totalSessions: summary.value.totalSessions,
  }));

  function toPairs(list: any[] | undefined) {
    return (list || []).map((x) => ({ name: String(x?.k ?? '-'), value: Number(x?.c) || 0 }));
  }

  async function loadStats() {
    statsLoading.value = true;
    try {
      const res: any = await getSessionStats();
      stats.value = res || {};

      const askData = toPairs(res?.askDist);
      askChart.setOptions({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 8, right: 16, top: 16, bottom: 8, containLabel: true },
        xAxis: { type: 'category', data: askData.map((d) => `${d.name} 轮`) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{ type: 'bar', data: askData.map((d) => d.value), barMaxWidth: 26, itemStyle: { color: '#fa8c16' } }],
      });

      const trendData = toPairs(res?.sessionTrend);
      trendChart.setOptions({
        tooltip: { trigger: 'axis' },
        grid: { left: 8, right: 16, top: 16, bottom: 8, containLabel: true },
        xAxis: { type: 'category', boundaryGap: false, data: trendData.map((d) => d.name) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{ type: 'line', smooth: true, data: trendData.map((d) => d.value), itemStyle: { color: '#1677ff' }, areaStyle: { opacity: 0.15 } }],
      });

      const modeData = toPairs(res?.modeDist).map((d) => ({ name: modeMap[d.name] || d.name, value: d.value }));
      modeChart.setOptions({
        tooltip: { trigger: 'item', formatter: '{b}: {c}（{d}%）' },
        legend: { bottom: 0 },
        series: [{ type: 'pie', radius: ['35%', '65%'], center: ['50%', '45%'], data: modeData, label: { formatter: '{b}: {c}' } }],
      });
    } finally {
      statsLoading.value = false;
    }
  }

  // ---------- 会话列表 ----------
  const [registerTable] = useTable({
    title: 'AI 会话列表',
    api: getSessionList,
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

  // ---------- 会话详情回放（子组件 SessionDetailDrawer 经 useDrawerInner 取数） ----------
  const [registerDrawer, { openDrawer }] = useDrawer();

  function openDetail(record: Recordable) {
    openDrawer(true, { record });
  }

  // 支持 ItemDetailDrawer 时间线跳转：/ailing/process?sessionId=xxx
  onMounted(async () => {
    loadStats();
    const sessionId = route.query.sessionId;
    if (sessionId) {
      openDetail({ id: String(sessionId) });
    }
  });
</script>

<style lang="less" scoped>
  .ailing-process {
    background: #fff;
    border-radius: 8px;
  }
</style>
