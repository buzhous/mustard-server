<template>
  <div class="ailing-stats" style="padding: 16px">
    <a-spin :spinning="loading">
      <a-row :gutter="16">
        <a-col v-for="card in summaryCards" :key="card.label" :span="6">
          <a-card size="small">
            <a-statistic :title="card.label" :value="card.value" :suffix="card.suffix" />
          </a-card>
        </a-col>
      </a-row>
      <a-row :gutter="16" style="margin-top: 16px">
        <a-col :span="12">
          <a-card size="small" title="分类 TOP10（点击筛选物品列表）">
            <div ref="catRef" style="height: 320px; width: 100%"></div>
          </a-card>
        </a-col>
        <a-col :span="12">
          <a-card size="small" title="标签 TOP10（点击筛选物品列表）">
            <div ref="tagRef" style="height: 320px; width: 100%"></div>
          </a-card>
        </a-col>
      </a-row>
      <a-row :gutter="16" style="margin-top: 16px">
        <a-col :span="8">
          <a-card size="small" title="等级分布（点击筛选物品列表）">
            <div ref="levelRef" style="height: 300px; width: 100%"></div>
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card size="small" title="建档趋势（近30天，点击筛选物品列表）">
            <div ref="trendRef" style="height: 300px; width: 100%"></div>
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card size="small" title="来源分布（点击筛选物品列表）">
            <div ref="sourceRef" style="height: 300px; width: 100%"></div>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<script lang="ts" name="ailing-stats" setup>
  import { Ref, computed, onMounted, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { useECharts } from '/@/hooks/web/useECharts';
  import { getItemStats } from '../items/api';
  import { sourceMap } from '../items/data';

  const router = useRouter();
  const loading = ref(false);
  const stats = ref<Recordable>({});

  const catRef = ref<HTMLDivElement | null>(null);
  const tagRef = ref<HTMLDivElement | null>(null);
  const levelRef = ref<HTMLDivElement | null>(null);
  const trendRef = ref<HTMLDivElement | null>(null);
  const sourceRef = ref<HTMLDivElement | null>(null);
  const catChart = useECharts(catRef as Ref<HTMLDivElement>);
  const tagChart = useECharts(tagRef as Ref<HTMLDivElement>);
  const levelChart = useECharts(levelRef as Ref<HTMLDivElement>);
  const trendChart = useECharts(trendRef as Ref<HTMLDivElement>);
  const sourceChart = useECharts(sourceRef as Ref<HTMLDivElement>);

  const summary = computed<Recordable>(() => stats.value?.summary || {});

  const summaryCards = computed(() => {
    const ratio = Number(summary.value.aiRatio);
    const ratioOk = !isNaN(ratio);
    return [
      { label: '物品总量', value: summary.value.total ?? '-', suffix: '' },
      { label: '建档用户数', value: summary.value.userCount ?? '-', suffix: '' },
      { label: '今日新增', value: summary.value.todayNew ?? '-', suffix: '' },
      { label: 'AI建档占比', value: ratioOk ? (ratio <= 1 ? ratio * 100 : ratio).toFixed(2) : '-', suffix: ratioOk ? '%' : '' },
    ];
  });

  // 图表项 {k, c} 统一转换
  function toPairs(list: any[] | undefined) {
    return (list || []).map((x) => ({ name: String(x?.k ?? '-'), value: Number(x?.c) || 0 }));
  }

  function barOptions(data: { name: string; value: number }[], color = '#1677ff') {
    return {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 8, right: 24, top: 16, bottom: 8, containLabel: true },
      xAxis: { type: 'value', minInterval: 1 },
      yAxis: { type: 'category', inverse: true, data: data.map((d) => d.name) },
      series: [{ type: 'bar', data: data.map((d) => d.value), barMaxWidth: 18, itemStyle: { color } }],
    };
  }

  function bindChartClick(chart: ReturnType<typeof useECharts>, handler: (name: string) => void) {
    const inst = chart.getInstance();
    if (!inst) return;
    inst.off('click');
    inst.on('click', (params: any) => {
      if (params?.name != null) handler(String(params.name));
    });
  }

  // 钻取跳转：stats 图项 → items 列表页带筛选
  function drill(query: Recordable) {
    router.push({ path: '/ailing/items', query });
  }

  async function load() {
    loading.value = true;
    try {
      const res: any = await getItemStats();
      stats.value = res || {};

      const catData = toPairs(res?.categoryTop).slice(0, 10);
      catChart.setOptions(barOptions(catData, '#1677ff'));
      bindChartClick(catChart, (name) => drill({ category: name }));

      const tagData = toPairs(res?.tagTop).slice(0, 10);
      tagChart.setOptions(barOptions(tagData, '#13c2c2'));
      bindChartClick(tagChart, (name) => drill({ tag: name }));

      const levelData = toPairs(res?.levelDist);
      levelChart.setOptions({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 8, right: 16, top: 16, bottom: 8, containLabel: true },
        xAxis: { type: 'category', data: levelData.map((d) => d.name) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{ type: 'bar', data: levelData.map((d) => d.value), barMaxWidth: 26, itemStyle: { color: '#52c41a' } }],
      });
      bindChartClick(levelChart, (name) => drill({ level: name }));

      const trendData = toPairs(res?.createdTrend);
      trendChart.setOptions({
        tooltip: { trigger: 'axis' },
        grid: { left: 8, right: 16, top: 16, bottom: 8, containLabel: true },
        xAxis: { type: 'category', boundaryGap: false, data: trendData.map((d) => d.name) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [
          { type: 'line', smooth: true, data: trendData.map((d) => d.value), itemStyle: { color: '#722ed1' }, areaStyle: { opacity: 0.15 } },
        ],
      });
      bindChartClick(trendChart, (name) => drill({ createTimeStart: name, createTimeEnd: name }));

      const sourceData = toPairs(res?.sourceDist).map((d) => ({ name: sourceMap[d.name] || d.name, value: d.value }));
      sourceChart.setOptions({
        tooltip: { trigger: 'item', formatter: '{b}: {c}（{d}%）' },
        legend: { bottom: 0 },
        series: [{ type: 'pie', radius: ['35%', '65%'], center: ['50%', '45%'], data: sourceData, label: { formatter: '{b}: {c}' } }],
      });
      bindChartClick(sourceChart, (name) => {
        const raw = Object.keys(sourceMap).find((k) => sourceMap[k] === name) || name;
        drill({ source: raw });
      });
    } finally {
      loading.value = false;
    }
  }

  onMounted(load);
</script>
