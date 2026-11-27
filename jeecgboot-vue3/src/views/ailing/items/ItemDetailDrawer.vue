<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" title="物品详情" width="780" destroyOnClose>
    <a-spin :spinning="loading">
      <a-alert v-if="chainValid === false" type="error" show-icon class="mb-2" message="账本校验异常：流转链哈希不连续，请核查数据完整性" />

      <!-- 基础信息 / 回收信息 -->
      <a-descriptions v-if="!isRecycle" title="基础信息" bordered size="small" :column="2">
        <a-descriptions-item v-for="d in descItems" :key="d.label" :label="d.label">
          <a-avatar v-if="d.icon && d.value" :src="String(d.value)" shape="square" :size="28" />
          <span v-else>{{ d.value ?? '-' }}</span>
        </a-descriptions-item>
      </a-descriptions>
      <a-descriptions v-else title="回收信息" bordered size="small" :column="2">
        <a-descriptions-item v-for="d in recycleDesc" :key="d.label" :label="d.label">{{ d.value ?? '-' }}</a-descriptions-item>
      </a-descriptions>

      <!-- 组件字段（masked 已由服务端脱敏） -->
      <a-divider>组件字段{{ fieldRows.length ? `（${fieldRows.length}）` : '' }}</a-divider>
      <a-empty v-if="!fieldRows.length" description="无组件字段" />
      <a-descriptions v-else bordered size="small" :column="1">
        <a-descriptions-item v-for="(f, i) in fieldRows" :key="i" :label="String(f.name || `组件#${f.element}`)">
          <a-tag v-if="f.masked" color="orange">脱敏</a-tag>
          <a-tag v-else-if="f.sensitivity && f.sensitivity !== 'normal'" color="red">{{ f.sensitivity }}</a-tag>
          <span :style="f.masked ? 'color:#999' : ''">{{ f.masked ? '手动补录' : formatFieldContent(f.content) }}</span>
        </a-descriptions-item>
      </a-descriptions>

      <!-- 流转时间线（账本 + AI 操作双源归一，按时间合并） -->
      <a-divider>流转时间线{{ mergedEvents.length ? `（${mergedEvents.length}）` : '' }}</a-divider>
      <a-timeline v-if="mergedEvents.length">
        <a-timeline-item v-for="(e, i) in mergedEvents" :key="i" :color="e.source === 'ai' ? 'purple' : eventColorMap[e.event_type] || 'blue'">
          <p style="margin: 0">
            <template v-if="e.source === 'ai'">
              <a-tag color="purple">芥灵AI</a-tag>
              <a-tag :color="e.kind === 'receipt' ? 'green' : 'cyan'">{{ e.kind === 'receipt' ? 'AI建档回执' : 'AI建档' }}</a-tag>
              <a-tag v-if="e.express" color="gold">express</a-tag>
              <span v-if="e.qty != null">数量：{{ e.qty }}</span>
              <a @click="gotoSession(e.sessionId)" style="margin-left: 8px">会话 {{ e.sessionId }}</a>
            </template>
            <template v-else>
              <a-tag :color="eventColorMap[e.event_type] || 'blue'">{{ eventMap[e.event_type] || e.event_type || '-' }}</a-tag>
              <span v-if="e.operator">操作人：{{ e.operator }}</span>
              <span v-if="e.qty != null" style="margin-left: 8px">数量：{{ e.qty }}</span>
            </template>
            <span style="margin-left: 8px; color: #999">{{ e.source === 'ai' ? e.time : e.occurred_at }}</span>
          </p>
          <p v-if="e.source !== 'ai' && e.hash" style="margin: 0; font-size: 12px; color: #bbb">
            hash: {{ shortHash(e.hash) }}<template v-if="e.prev_hash"> ← prev: {{ shortHash(e.prev_hash) }}</template>
          </p>
          <p v-if="e.source !== 'ai' && payloadText(e)" style="margin: 0; font-size: 12px; color: #999; word-break: break-all">payload: {{ payloadText(e) }}</p>
        </a-timeline-item>
      </a-timeline>
      <a-empty v-else description="无流转记录" />

      <!-- 派生子链 -->
      <a-divider>派生子链{{ spawnChildren.length ? `（${spawnChildren.length}）` : '' }}</a-divider>
      <template v-if="spawnChildren.length">
        <div v-for="(c, i) in spawnChildren" :key="i" style="margin-bottom: 4px">
          <a-tag color="purple">派生</a-tag>
          {{ c.name || '-' }}（{{ c.ori_id || c.id || '-' }}）
        </div>
      </template>
      <a-empty v-else description="无派生子链" />
    </a-spin>
  </BasicDrawer>
</template>

<script lang="ts" name="ailing-item-detail-drawer" setup>
  import { computed, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { getItemDetail, getItemTimeline } from './api';
  import { eventMap, eventColorMap, formatFieldContent, sourceMap, statusMap } from './data';

  const router = useRouter();
  const loading = ref(false);
  const isRecycle = ref(false);
  const detail = ref<Recordable>({});
  const recycle = ref<Recordable>({});
  const events = ref<Recordable[]>([]);
  const aiEvents = ref<Recordable[]>([]);
  const spawnChildren = ref<Recordable[]>([]);
  const chainValid = ref<boolean | null>(null);

  /** 双源归一：账本事件（occurred_at）+ AI 操作事件（time）按时间合并 */
  const mergedEvents = computed<Recordable[]>(() => {
    const ledger = events.value.map((e) => ({ ...e, source: 'lineage' }));
    const ai = aiEvents.value.map((e) => ({ ...e, source: 'ai' }));
    return [...ledger, ...ai].sort((a, b) => String(a.occurred_at ?? a.time ?? '').localeCompare(String(b.occurred_at ?? b.time ?? '')));
  });

  /** 会话ID跳芥灵过程页回放 */
  function gotoSession(sessionId?: string) {
    if (sessionId) {
      router.push({ path: '/ailing/process', query: { sessionId } });
    }
  }

  const descItems = computed(() => {
    const d = detail.value || {};
    const it = (label: string, value: any, icon = false) => ({ label, value, icon });
    return [
      it('物品ID', d.id),
      it('OriID', d.ori_id ?? d.oriId),
      it('图标', d.icon, true),
      it('名称', d.name),
      it('分类', d.category),
      it('等级', d.level),
      it('数量', d.quantity),
      it('价格', d.price),
      it('状态', d.status != null ? statusMap[String(d.status)] ?? d.status : null),
      it('来源', d.source ? sourceMap[d.source] ?? d.source : null),
      it('所属用户', d.user_id),
      it('所属据点', d.station_id),
      it('据点类型', d.station_type),
      it('建档时间', d.create_time),
      it('更新时间', d.update_time),
    ].filter((x) => x.value !== undefined && x.value !== null && x.value !== '');
  });

  const recycleDesc = computed(() => {
    const r = recycle.value || {};
    const labelMap: Record<string, string> = {
      item_id: '物品ID',
      name: '名称',
      icon: '图标',
      category_id: '分类ID',
      quantity: '数量',
      level: '等级',
      price: '价格',
      status: '状态',
      source: '来源',
      user_id: '所属用户',
      create_time: '回收时间',
    };
    return Object.keys(r)
      .filter((k) => r[k] !== null && r[k] !== undefined && r[k] !== '')
      .map((k) => ({
        label: labelMap[k] || k,
        value: typeof r[k] === 'object' ? JSON.stringify(r[k]) : String(r[k]),
      }));
  });

  const fieldRows = computed<any[]>(() => {
    const f = detail.value?.fields;
    if (!f) return [];
    if (Array.isArray(f)) return f;
    if (typeof f === 'string') {
      try {
        return JSON.parse(f) || [];
      } catch {
        return [];
      }
    }
    return [];
  });

  function shortHash(hash?: string): string {
    if (!hash) return '-';
    return hash.length > 10 ? `${hash.slice(0, 10)}…` : hash;
  }

  function payloadText(e: Recordable): string {
    const p = e?.payload;
    if (!p) return '';
    let text = '';
    if (typeof p === 'string') {
      text = p;
    } else {
      try {
        text = JSON.stringify(p);
      } catch {
        text = String(p);
      }
    }
    return text.length > 120 ? `${text.slice(0, 120)}…` : text;
  }

  const [registerDrawer] = useDrawerInner(async (data) => {
    const record = data?.record || {};
    const itemId = record.item_id ?? record.id;
    isRecycle.value = false;
    detail.value = {};
    recycle.value = {};
    events.value = [];
    aiEvents.value = [];
    spawnChildren.value = [];
    chainValid.value = null;
    if (!itemId) return;
    loading.value = true;
    try {
      const d: any = await getItemDetail({ itemId }).catch(() => null);
      if (d?.view === 'recycle') {
        isRecycle.value = true;
        recycle.value = d.recycle || {};
      } else {
        detail.value = d || {};
      }
      const t: any = await getItemTimeline({ itemId }).catch(() => null);
      if (t) {
        chainValid.value = t.chainValid;
        events.value = t.events || [];
        aiEvents.value = t.aiEvents || [];
        spawnChildren.value = t.spawnChildren || [];
      }
    } finally {
      loading.value = false;
    }
  });
</script>
