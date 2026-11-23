<template>
  <div class="stats-dashboard">
    <!-- 顶部 KPI 卡 -->
    <a-card :bordered="false" class="block" :loading="overviewLoading">
      <div class="kpi-row">
        <div v-for="k in kpiItems" :key="k.field" class="kpi-item">
          <div class="kpi-icon" :style="{ color: k.color, backgroundColor: k.bg }">
            <Icon :icon="k.icon" :size="22" />
          </div>
          <div class="kpi-meta">
            <div class="kpi-value">{{ overview.today[k.field] ?? 0 }}</div>
            <div class="kpi-label">{{ k.label }}</div>
          </div>
        </div>
      </div>
    </a-card>

    <!-- 漏斗 + 注册来源 -->
    <a-row :gutter="12" class="block">
      <a-col :span="24">
        <a-card :bordered="false" :loading="overviewLoading" :title="`转化漏斗（近 ${days} 天）`">
          <div class="funnel-row">
            <div class="funnel-step">
              <div class="funnel-num">{{ overview.funnel.launch ?? 0 }}</div>
              <div class="funnel-name">启动（曝光）</div>
            </div>
            <div class="funnel-arrow">
              <span class="funnel-rate">{{ overview.funnel.launchToRegister ?? 0 }}%</span>
              <Icon icon="ant-design:arrow-right-outlined" />
            </div>
            <div class="funnel-step">
              <div class="funnel-num">{{ overview.funnel.register ?? 0 }}</div>
              <div class="funnel-name">注册</div>
            </div>
            <div class="funnel-arrow">
              <span class="funnel-rate">{{ overview.funnel.registerToActivate ?? 0 }}%</span>
              <Icon icon="ant-design:arrow-right-outlined" />
            </div>
            <div class="funnel-step">
              <div class="funnel-num">{{ overview.funnel.activate ?? 0 }}</div>
              <div class="funnel-name">激活（首次建档）</div>
            </div>
          </div>
          <!-- 漏斗条形填充：只做视觉填充，不改动上方统计数字 -->
          <div class="funnel-bars">
            <div v-for="b in funnelBars" :key="b.label" class="funnel-bar-row">
              <span class="funnel-bar-label">{{ b.label }}</span>
              <div class="funnel-bar-track">
                <div class="funnel-bar" :class="{ 'funnel-bar-empty': b.empty }" :style="{ width: b.pct + '%' }"></div>
              </div>
              <span class="funnel-bar-pct">{{ b.empty ? '—' : b.pct + '%' }}</span>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :span="24">
        <a-card :bordered="false" :loading="overviewLoading" :title="`注册来源（近 ${days} 天）`">
          <div v-if="sourceEntries.length === 0" class="empty-tip">暂无数据</div>
          <div v-for="s in sourceEntries" :key="s.key" class="source-row">
            <span class="source-label">{{ s.label }}</span>
            <div class="source-bar-track">
              <div class="source-bar" :style="{ width: s.pct + '%' }"></div>
            </div>
            <span class="source-value">{{ s.value }}</span>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 趋势图 -->
    <a-card title="趋势" :bordered="false" class="block">
      <template #extra>
        <div class="trend-toolbar">
          <a-select v-model:value="metric" :options="metricOptions" style="width: 180px" @change="loadTrend" />
          <a-radio-group v-model:value="days" button-style="solid" @change="handleDaysChange">
            <a-radio-button :value="7">近7天</a-radio-button>
            <a-radio-button :value="30">近30天</a-radio-button>
          </a-radio-group>
        </div>
      </template>
      <a-spin :spinning="trendLoading">
        <div ref="trendRef" style="width: 100%; height: 300px"></div>
      </a-spin>
    </a-card>

    <!-- 留存表 -->
    <a-card title="留存（按注册日 Cohort）" :bordered="false" class="block">
      <a-table
        :columns="retentionColumns"
        :data-source="retentionRows"
        :loading="retentionLoading"
        :pagination="false"
        size="small"
        row-key="cohortDate"
        :scroll="{ x: 560 }"
      />
    </a-card>

    <!-- 事件明细 -->
    <a-card title="事件明细" :bordered="false" class="block">
      <BasicTable @register="registerTable">
        <template #tableTitle>
          <a-button type="primary" pre-icon="ant-design:download-outlined" :loading="exporting" @click="handleExport">导出CSV</a-button>
        </template>
      </BasicTable>
    </a-card>
  </div>
</template>

<script lang="ts" name="stats-dashboard" setup>
  import { computed, onMounted, ref } from 'vue';
  import dayjs from 'dayjs';
  import { Icon } from '/@/components/Icon';
  import { BasicTable, BasicColumn } from '/@/components/Table';
  import type { FormSchema } from '/@/components/Form';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useECharts } from '/@/hooks/web/useECharts';
  import { getOverview, getTrend, getEvents, getRetention, exportStatsCsv } from '/@/api/stats/statsAdminApi';

  const { createMessage } = useMessage();

  // ==================== KPI 总览 ====================

  interface OverviewData {
    today: Record<string, number>;
    funnel: Record<string, number>;
    sums: Record<string, number>;
    registerSources: Record<string, number>;
  }

  const overview = ref<OverviewData>({ today: {}, funnel: {}, sums: {}, registerSources: {} });
  const overviewLoading = ref(false);
  const days = ref(30);

  const kpiItems = [
    { label: '今日 DAU', field: 'dau', icon: 'ant-design:team-outlined', color: '#1890ff', bg: '#e6f4ff' },
    { label: '新增注册', field: 'newUsers', icon: 'ant-design:user-add-outlined', color: '#52c41a', bg: '#f6ffed' },
    { label: '启动设备数', field: 'launchDevices', icon: 'ant-design:mobile-outlined', color: '#722ed1', bg: '#f9f0ff' },
    { label: '首次建档数', field: 'first_item_created', icon: 'ant-design:profile-outlined', color: '#fa8c16', bg: '#fff7e6' },
    { label: '支付成功数', field: 'payment_success', icon: 'ant-design:pay-circle-outlined', color: '#f5222d', bg: '#fff1f0' },
  ];

  const SOURCE_LABELS: Record<string, string> = { sms: '短信验证码', account: '账号密码', wechat: '微信', apple: 'Apple ID', unknown: '未知' };

  const sourceEntries = computed(() => {
    const entries = Object.entries(overview.value.registerSources || {});
    const max = Math.max(1, ...entries.map(([, v]) => Number(v)));
    return entries.map(([key, value]) => ({
      key,
      label: SOURCE_LABELS[key] || key,
      value: Number(value),
      pct: Math.round((Number(value) / max) * 100),
    }));
  });

  async function loadOverview() {
    overviewLoading.value = true;
    try {
      overview.value = (await getOverview({ days: days.value })) || overview.value;
    } finally {
      overviewLoading.value = false;
    }
  }

  // 漏斗条形（视觉填充）：宽度按各步与曝光的占比递减，形成漏斗轮廓；无数据时显示占位空条
  const funnelBars = computed(() => {
    const f = overview.value.funnel || {};
    const launch = Number(f.launch ?? 0);
    const register = Number(f.register ?? 0);
    const activate = Number(f.activate ?? 0);
    const pct = (v: number) => (launch <= 0 ? 0 : Math.max(6, Math.round((v / launch) * 100)));
    const regPct = launch <= 0 ? 0 : Math.round(f.launchToRegister ?? 0);
    const actPct = launch <= 0 ? 0 : Math.round((register <= 0 ? 0 : (activate / register) * 100));
    return [
      { label: '启动', pct: Math.max(6, pct(launch)), empty: launch <= 0 },
      { label: '注册', pct: Math.max(6, regPct), empty: launch <= 0 || register <= 0 },
      { label: '激活', pct: Math.max(6, actPct), empty: launch <= 0 || activate <= 0 },
    ];
  });

  // ==================== 趋势图 ====================

  const metricOptions = [
    { label: '今日活跃（DAU）', value: 'dau' },
    { label: '新增注册', value: 'new_users' },
    { label: '启动', value: 'event:app_launch' },
    { label: '注册', value: 'event:register' },
    { label: '首次建档', value: 'event:first_item_created' },
    { label: '支付成功', value: 'event:payment_success' },
  ];
  const metric = ref('dau');
  const trendLoading = ref(false);
  const trendRef = ref<HTMLDivElement | null>(null);
  const { setOptions } = useECharts(trendRef as any);

  async function loadTrend() {
    trendLoading.value = true;
    try {
      const list: Array<Record<string, any>> = (await getTrend({ metric: metric.value, days: days.value })) || [];
      // 接口只返回有数据的日子，按日期范围补零保证折线连续
      const map = new Map<string, number>();
      list.forEach((it) => map.set(String(it.stats_date).slice(0, 10), Number(it.value ?? 0)));
      const dates: string[] = [];
      const values: number[] = [];
      for (let i = days.value - 1; i >= 0; i--) {
        const d = dayjs().subtract(i, 'day');
        dates.push(d.format('MM-DD'));
        values.push(map.get(d.format('YYYY-MM-DD')) ?? 0);
      }
      const label = metricOptions.find((o) => o.value === metric.value)?.label || metric.value;
      setOptions({
        tooltip: { trigger: 'axis' },
        grid: { left: 48, right: 24, top: 36, bottom: 28 },
        xAxis: { type: 'category', boundaryGap: false, data: dates },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{ name: label, type: 'line', smooth: true, data: values, areaStyle: { opacity: 0.15 }, showSymbol: days.value <= 7 }],
      });
    } finally {
      trendLoading.value = false;
    }
  }

  async function handleDaysChange() {
    await Promise.all([loadOverview(), loadTrend()]);
  }

  // ==================== 留存表 ====================

  const retentionLoading = ref(false);
  const retentionList = ref<Array<Record<string, any>>>([]);
  const retentionColumns = [
    { title: '注册日（Cohort）', dataIndex: 'cohortDate', width: 140 },
    { title: '当日', dataIndex: 'd0', width: 90 },
    { title: '次日', dataIndex: 'd1', width: 90 },
    { title: '3日', dataIndex: 'd3', width: 90 },
    { title: '7日', dataIndex: 'd7', width: 90 },
  ];

  const retentionRows = computed(() =>
    retentionList.value.map((it) => {
      const offsets = it.offsets || {};
      const val = (offset: string) => (offsets[offset] === undefined || offsets[offset] === null ? '-' : offsets[offset]);
      return { cohortDate: it.cohortDate, d0: val('0'), d1: val('1'), d3: val('3'), d7: val('7') };
    })
  );

  async function loadRetention() {
    retentionLoading.value = true;
    try {
      retentionList.value = (await getRetention({ days: 14 })) || [];
    } finally {
      retentionLoading.value = false;
    }
  }

  // ==================== 事件明细 ====================

  const eventOptions = [
    'app_launch',
    'register',
    'first_item_created',
    'coupon_claimed',
    'pickup_claimed',
    'gift_sent',
    'appkey_activated',
    'payment_success',
  ].map((e) => ({ label: e, value: e }));

  const searchFormSchema: FormSchema[] = [
    {
      label: '日期',
      field: 'date',
      component: 'DatePicker',
      componentProps: { valueFormat: 'YYYY-MM-DD', format: 'YYYY-MM-DD', placeholder: '请选择日期', allowClear: true },
      colProps: { span: 6 },
    },
    {
      label: '事件',
      field: 'event',
      component: 'Select',
      componentProps: { options: eventOptions, placeholder: '请选择事件', allowClear: true },
      colProps: { span: 6 },
    },
  ];

  function formatTs(text) {
    return text ? dayjs(Number(text)).format('YYYY-MM-DD HH:mm:ss') : '';
  }

  const eventColumns: BasicColumn[] = [
    { title: 'ID', dataIndex: 'id', width: 90 },
    { title: '事件ID', dataIndex: 'eventId', width: 240 },
    { title: '事件', dataIndex: 'event', width: 150 },
    { title: '用户', dataIndex: 'uid', width: 120, customRender: ({ text }) => (text ? String(text) : '匿名') },
    { title: '设备ID', dataIndex: 'deviceId', width: 200 },
    { title: '平台', dataIndex: 'platform', width: 90, customRender: ({ text }) => text || '—' },
    { title: '版本', dataIndex: 'appVersion', width: 100, customRender: ({ text }) => text || '—' },
    { title: '客户端时间', dataIndex: 'clientTs', width: 160, customRender: ({ text }) => formatTs(text) },
    { title: '服务端时间', dataIndex: 'serverTs', width: 160, customRender: ({ text }) => formatTs(text) },
    { title: '扩展属性', dataIndex: 'props' },
  ];

  const { tableContext } = useListPage({
    designScope: 'stats-dashboard',
    tableProps: {
      title: '事件明细',
      api: getEvents,
      columns: eventColumns,
      showActionColumn: false,
      scroll: { x: 1400 },
      formConfig: { labelWidth: 60, schemas: searchFormSchema, autoSubmitOnEnter: true },
      beforeFetch: (params) => params,
    },
  });
  const [registerTable] = tableContext;

  const exporting = ref(false);

  async function handleExport() {
    exporting.value = true;
    try {
      const res: any = await exportStatsCsv({ days: days.value });
      const blob = new Blob(['\ufeff' + String(res?.data ?? '')], { type: 'text/csv;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `运营统计_日汇总_${dayjs().format('YYYYMMDD_HHmmss')}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      createMessage.success('导出成功');
    } finally {
      exporting.value = false;
    }
  }

  // ==================== 初始化 ====================

  onMounted(() => {
    loadOverview();
    loadTrend();
    loadRetention();
  });
</script>

<style lang="less" scoped>
  .stats-dashboard {
    padding: 8px;

    .block {
      margin-bottom: 12px;
    }

    .kpi-row {
      display: flex;
      gap: 16px;
      flex-wrap: wrap;
    }

    .kpi-item {
      flex: 1;
      min-width: 150px;
      display: flex;
      align-items: center;

      .kpi-icon {
        width: 48px;
        height: 48px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 12px;
        flex-shrink: 0;
      }

      .kpi-value {
        font-size: 24px;
        font-weight: 600;
        line-height: 1.3;
      }

      .kpi-label {
        color: #999;
        font-size: 13px;
      }
    }

    .funnel-row {
      display: flex;
      align-items: center;
      justify-content: space-around;
      flex-wrap: wrap;
      gap: 10px 14px;
      padding: 24px 12px;

      .funnel-step {
        text-align: center;
        min-width: 0;

        .funnel-num {
          font-size: clamp(18px, 2.6vw, 26px);
          font-weight: 600;
        }

        .funnel-name {
          color: #999;
          font-size: clamp(11px, 1.3vw, 13px);
          margin-top: 4px;
        }
      }

      .funnel-arrow {
        display: flex;
        flex-direction: column;
        align-items: center;
        color: #1890ff;
        flex-shrink: 0;

        .funnel-rate {
          font-size: clamp(11px, 1.3vw, 13px);
          margin-bottom: 2px;
        }
      }
    }

    .source-row {
      display: flex;
      align-items: center;
      margin-bottom: 14px;

      .source-label {
        width: 90px;
        color: #666;
        flex-shrink: 0;
      }

      .source-bar-track {
        flex: 1;
        height: 12px;
        background: #f0f0f0;
        border-radius: 6px;
        overflow: hidden;
        margin-right: 12px;

        .source-bar {
          height: 100%;
          background: linear-gradient(90deg, #36a3f7, #1890ff);
          border-radius: 6px;
          transition: width 0.3s;
        }
      }

      .source-value {
        width: 70px;
        text-align: right;
      }
    }

    .funnel-bars {
      margin-top: 4px;
      padding: 0 12px 8px;
      border-top: 1px dashed #eee;

      .funnel-bar-row {
        display: flex;
        align-items: center;
        margin-top: 10px;

        .funnel-bar-label {
          width: 44px;
          color: #666;
          font-size: 12px;
          flex-shrink: 0;
        }

        .funnel-bar-track {
          flex: 1;
          height: 18px;
          background: #f5f7fa;
          border-radius: 9px;
          overflow: hidden;

          .funnel-bar {
            height: 100%;
            border-radius: 9px;
            background: linear-gradient(90deg, #36a3f7, #1890ff);
            transition: width 0.3s;

            &.funnel-bar-empty {
              background: repeating-linear-gradient(45deg, #e8ecf2, #e8ecf2 6px, #f2f5f9 6px, #f2f5f9 12px);
            }
          }
        }

        .funnel-bar-pct {
          width: 52px;
          text-align: right;
          color: #1890ff;
          font-size: 12px;
          flex-shrink: 0;
        }
      }
    }

    .empty-tip {
      color: #999;
      text-align: center;
      padding: 24px 0;
    }

    .trend-toolbar {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }
</style>
