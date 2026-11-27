<template>
  <div class="session-replay">
    <div v-for="(m, i) in messages" :key="m.id ?? i" class="msg-row" :class="isUserMsg(m.role) ? 'msg-user' : 'msg-ai'">
      <!-- 用户文本气泡 -->
      <div v-if="isUserMsg(m.role)" class="bubble bubble-user">
        <p class="bubble-text">{{ m.content || '-' }}</p>
        <div v-if="imageCount(m) > 0" class="msg-images"><a-tag color="blue">图片×{{ imageCount(m) }}</a-tag></div>
        <div class="msg-time">{{ m.create_time }}</div>
      </div>

      <!-- AI 消息（6 类卡片） -->
      <div v-else class="bubble bubble-ai">
        <!-- text -->
        <template v-if="m.msg_type === 'text'">
          <p class="bubble-text">{{ m.content || '-' }}</p>
        </template>

        <!-- clarify 追问 -->
        <template v-else-if="m.msg_type === 'clarify'">
          <a-tag color="orange">追问</a-tag>
          <p class="bubble-text" style="margin-top: 4px">{{ m.content || '-' }}</p>
        </template>

        <!-- draft 草稿卡 -->
        <template v-else-if="m.msg_type === 'draft'">
          <a-tag color="purple">草稿卡</a-tag>
          <a-descriptions bordered size="small" :column="2" class="card-box">
            <a-descriptions-item label="名称">{{ slotValue(draftOf(m)?.name) || '-' }}</a-descriptions-item>
            <a-descriptions-item label="分类">{{ slotValue(draftOf(m)?.category) || '-' }}</a-descriptions-item>
            <a-descriptions-item label="数量">{{ slotValue(draftOf(m)?.quantity) || 1 }}</a-descriptions-item>
            <a-descriptions-item label="整体置信度">
              <a-tag :color="confColor(m.confidence)">{{ m.confidence ?? '-' }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="标签" :span="2">
              <template v-if="draftTags(draftOf(m)?.tags).length">
                <a-tag v-for="t in draftTags(draftOf(m)?.tags)" :key="t" color="blue" style="margin-inline-end: 4px">{{ t }}</a-tag>
              </template>
              <span v-else>-</span>
            </a-descriptions-item>
            <a-descriptions-item v-if="manualFillOf(m).length" label="手动补录提示" :span="2">
              <a-tag v-for="t in manualFillOf(m)" :key="t" color="red" style="margin-inline-end: 4px">{{ t }}</a-tag>
            </a-descriptions-item>
          </a-descriptions>
        </template>

        <!-- process 过程条目 -->
        <template v-else-if="m.msg_type === 'process'">
          <a-tag color="cyan">过程</a-tag>
          <a-steps size="small" :current="2" class="card-box" :items="processStepsOf(m)"></a-steps>
          <p class="card-extra">
            总数 {{ boxOf(m)?.total ?? '-' }} · 成功 {{ boxOf(m)?.ok ?? '-' }} · 失败 {{ boxOf(m)?.fail ?? '-' }}
            <a-tag v-if="boxOf(m)?.express" color="gold" style="margin-left: 8px">express</a-tag>
          </p>
          <p v-if="itemIdsOf(m).length" class="card-extra">
            itemId：<a v-for="id in itemIdsOf(m)" :key="id" @click="emit('gotoItem', String(id))" style="margin-right: 8px">{{ id }}</a>
          </p>
        </template>

        <!-- receipt 回执卡 -->
        <template v-else-if="m.msg_type === 'receipt'">
          <a-tag color="green">回执</a-tag>
          <p class="bubble-text" style="margin-top: 4px">
            {{ m.content || '-' }}
            <a-tag v-if="boxOf(m)?.express" color="gold" style="margin-left: 8px">express 直达</a-tag>
          </p>
          <p v-if="itemIdsOf(m).length" class="card-extra">
            itemId：<a v-for="id in itemIdsOf(m)" :key="id" @click="emit('gotoItem', String(id))" style="margin-right: 8px">{{ id }}</a>
          </p>
        </template>

        <!-- query_result 查询结果卡 -->
        <template v-else-if="m.msg_type === 'query_result'">
          <a-tag color="blue">查询卡</a-tag>
          <p class="bubble-text" style="margin-top: 4px">{{ m.content || '-' }}</p>
          <a-descriptions v-if="cardSummaryOf(m).rows.length" bordered size="small" :column="1" class="card-box">
            <a-descriptions-item v-for="(r, ri) in cardSummaryOf(m).rows" :key="ri" :label="String(ri + 1)">{{ r }}</a-descriptions-item>
          </a-descriptions>
          <p class="card-extra">类型：{{ cardSummaryOf(m).type }}</p>
        </template>

        <!-- 未知类型兜底 -->
        <template v-else>
          <a-tag>未知类型</a-tag>
          <p class="bubble-text" style="margin-top: 4px">{{ m.content || '-' }}</p>
        </template>

        <div class="msg-time">{{ m.create_time }}</div>
      </div>
    </div>
    <a-empty v-if="!messages.length" description="无消息记录" />
  </div>
</template>

<script lang="ts" name="ailing-session-replay" setup>
  import { draftTags, isUserMsg, parseJsonField, queryCardSummary, slotValue } from './data';

  defineProps<{ messages: Recordable[] }>();
  const emit = defineEmits<{ (e: 'gotoItem', itemId: string): void }>();

  function confColor(c: any): string {
    const n = Number(c);
    if (isNaN(n)) return 'default';
    return n >= 85 ? 'green' : n >= 60 ? 'gold' : 'red';
  }

  function imageCount(m: Recordable): number {
    const arr = parseJsonField(m.images);
    return Array.isArray(arr) ? arr.length : 0;
  }

  /** 过程/回执卡 JSON（process_entries），查询卡复用 draft_snapshot（queryCardSummary 已按卡型取行） */
  function boxOf(m: Recordable) {
    return parseJsonField(m.process_entries);
  }
  function draftOf(m: Recordable) {
    return parseJsonField(m.draft_snapshot);
  }
  function manualFillOf(m: Recordable): string[] {
    const arr = parseJsonField(draftOf(m)?.manualFill);
    return Array.isArray(arr) ? arr.map(String) : [];
  }
  function itemIdsOf(m: Recordable): string[] {
    const arr = parseJsonField(boxOf(m)?.itemIds);
    return Array.isArray(arr) ? arr.map(String) : [];
  }
  function processStepsOf(m: Recordable) {
    const b = boxOf(m);
    const ok = Number(b?.ok ?? 0);
    const fail = Number(b?.fail ?? 0);
    return [
      { title: '发起创建', description: `×${b?.total ?? '-'}` },
      { title: '执行', description: 'proposed + 创建' },
      { title: fail > 0 ? '部分失败' : '完成', description: `成功 ${ok} · 失败 ${fail}`, status: fail > 0 ? 'error' : 'finish' },
    ];
  }
  function cardSummaryOf(m: Recordable) {
    return queryCardSummary(parseJsonField(m.draft_snapshot));
  }
</script>

<style lang="less" scoped>
  .session-replay {
    .msg-row {
      display: flex;
      margin-bottom: 12px;
      &.msg-user {
        justify-content: flex-end;
      }
    }
    .bubble {
      max-width: 78%;
      padding: 8px 12px;
      border-radius: 8px;
      .bubble-text {
        margin: 0;
        word-break: break-all;
        white-space: pre-wrap;
      }
      .msg-time {
        margin-top: 4px;
        font-size: 12px;
        color: #bbb;
      }
      .card-box {
        margin-top: 8px;
      }
      .card-extra {
        margin: 6px 0 0;
        font-size: 12px;
        color: #666;
        word-break: break-all;
      }
    }
    .bubble-user {
      background: #e6f4ff;
    }
    .bubble-ai {
      background: #f6f8f6;
    }
  }
</style>
