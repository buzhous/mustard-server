<template>
  <div class="field-config-editor">
    <div class="fce-header">
      <span class="fce-title">物品字段配置</span>
      <span class="fce-tip">图标(1)、名称(2)、类别(3) 为必填三要素，不可移除；字段来源为基础配置 &gt; 字段管理字典，主题类型由字段配置自动推导</span>
    </div>
    <div class="fce-toolbar">
      <a-select
        v-model:value="addElementKey"
        show-search
        :options="addableOptions"
        placeholder="添加组件"
        style="width: 220px"
        @change="handleAddElement"
      />
      <span class="fce-type-label">主题类型（自动）：{{ derivedTypeLabel }}</span>
    </div>
    <div class="fce-rows">
      <div v-for="(row, index) in rows" :key="row.element" class="fce-row">
        <div class="fce-row-main">
          <span class="fce-element-tag">{{ elementLabel(row.element) }}</span>
          <a-input v-model:value="row.name" class="fce-name-input" placeholder="组件字段名" />
          <span class="fce-sort-label">排序 {{ index + 1 }}</span>
          <span class="fce-show-label">显示</span>
          <a-switch v-model:checked="row.isShow" size="small" />
          <span class="fce-row-actions">
            <a-button type="link" size="small" :disabled="index === 0" @click="moveRow(index, -1)">上移</a-button>
            <a-button type="link" size="small" :disabled="index === rows.length - 1" @click="moveRow(index, 1)">下移</a-button>
            <a-button type="link" size="small" @click="row.advancedVisible = !row.advancedVisible">
              {{ row.advancedVisible ? '收起' : '高级(JSON)' }}
            </a-button>
            <a-button
              v-if="!isRequiredElement(row.element)"
              type="link"
              size="small"
              danger
              @click="removeRow(index)"
            >
              移除
            </a-button>
            <a-tooltip v-else title="必填三要素不可移除">
              <a-button type="link" size="small" disabled>移除</a-button>
            </a-tooltip>
          </span>
        </div>
        <div v-if="row.advancedVisible" class="fce-row-advanced">
          <div class="fce-json-block">
            <div class="fce-json-label">组件属性 attributes（JSON 对象，可空）</div>
            <a-textarea v-model:value="row.attributesText" :rows="3" placeholder='如 {"placeholder": "请输入名称"}' />
          </div>
          <div class="fce-json-block">
            <div class="fce-json-label">组件内容 content（JSON 对象，可空）</div>
            <a-textarea v-model:value="row.contentText" :rows="3" placeholder='如 {"text": "默认文本"}' />
          </div>
        </div>
      </div>
      <div v-if="rows.length === 0" class="fce-empty">请先添加组件</div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, watch, computed, onMounted } from 'vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getFieldElementOptions, FieldElementOption } from '/@/api/base/fieldElementApi';

  interface RowItem {
    element: number;
    name: string;
    isShow: boolean;
    attributesText: string;
    contentText: string;
    advancedVisible: boolean;
  }

  const props = defineProps<{
    value?: string;
  }>();

  const emit = defineEmits(['update:value', 'type-change']);

  /** 字段字典选项：来自基础配置 > 字段管理（/admin/fieldElement/list），挂载时动态加载 */
  const dictOptions = ref<FieldElementOption[]>([]);
  const { createMessage } = useMessage();

  onMounted(async () => {
    try {
      dictOptions.value = await getFieldElementOptions();
    } catch (e) {
      createMessage.error('字段字典加载失败，请刷新重试');
    }
  });

  const REQUIRED_ELEMENTS = [1, 2, 3];

  const rows = ref<RowItem[]>([]);
  const addElementKey = ref<number | undefined>(undefined);
  let lastEmitted = '';

  const isRequiredElement = (element: number) => REQUIRED_ELEMENTS.includes(element);
  const elementLabel = (element: number) =>
    dictOptions.value.find((item) => item.element === element)?.name || `组件${element}`;

  const addableOptions = computed(() =>
    dictOptions.value
      .filter((item) => item.status === 1 && !rows.value.some((row) => row.element === item.element))
      .map((item) => ({
        label: `${item.name}（${item.element}）`,
        value: item.element,
      })),
  );

  const derivedType = computed(() => {
    const elements = rows.value.map((row) => row.element);
    if (elements.includes(20)) return 3;
    if (elements.includes(12) || elements.includes(13) || elements.includes(14) || elements.includes(15)) return 2;
    if (elements.includes(17) || elements.includes(18) || elements.includes(19)) return 4;
    return 1;
  });

  const derivedTypeLabel = computed(() => ({ 1: '基础型', 2: '多媒体型', 3: '模型型', 4: '扩展型' }[derivedType.value] || '未知'));

  function createRow(element: number, name?: string, isShow = true, attributesText = '', contentText = ''): RowItem {
    const meta = dictOptions.value.find((item) => item.element === element);
    return {
      element,
      name: name ?? meta?.name ?? '',
      isShow,
      attributesText,
      contentText,
      advancedVisible: false,
    };
  }

  function syncFromValue(value?: string) {
    // 自身 emit 导致的回写不再重建行（避免重置编辑状态）
    if (value === lastEmitted) return;
    if (!value) {
      // 默认预置必填三要素
      rows.value = REQUIRED_ELEMENTS.map((element) => createRow(element));
      emitValue();
      return;
    }
    try {
      const parsed = JSON.parse(value);
      if (!Array.isArray(parsed)) {
        rows.value = REQUIRED_ELEMENTS.map((element) => createRow(element));
        emitValue();
        return;
      }
      // 按组件类型去重（历史脏数据可能含重复组件，重复 key 会导致行无法编辑）
      const seen = new Set<number>();
      rows.value = parsed
        .filter((item) => item && typeof item.element === 'number')
        .filter((item) => {
          if (seen.has(item.element)) return false;
          seen.add(item.element);
          return true;
        })
        .map((item) =>
          createRow(
            item.element,
            typeof item.name === 'string' ? item.name : undefined,
            !(item.isShow === 0),
            item.attributes ? JSON.stringify(item.attributes) : '',
            item.content ? JSON.stringify(item.content) : '',
          ),
        );
      // 保证必填三要素存在
      for (const element of REQUIRED_ELEMENTS) {
        if (!rows.value.some((row) => row.element === element)) {
          rows.value.push(createRow(element));
        }
      }
    } catch (e) {
      rows.value = REQUIRED_ELEMENTS.map((element) => createRow(element));
      emitValue();
    }
  }

  function emitValue() {
    const payload = rows.value.map((row, index) => {
      const item: Record<string, unknown> = {
        element: row.element,
        name: row.name,
        sort: index + 1,
        isShow: row.isShow ? 1 : 0,
      };
      if (row.attributesText && row.attributesText.trim()) {
        try {
          item.attributes = JSON.parse(row.attributesText);
        } catch (e) {
          // 校验阶段会拦截，这里忽略
        }
      }
      if (row.contentText && row.contentText.trim()) {
        try {
          item.content = JSON.parse(row.contentText);
        } catch (e) {
          // 校验阶段会拦截，这里忽略
        }
      }
      return item;
    });
    const json = JSON.stringify(payload);
    lastEmitted = json;
    emit('update:value', json);
    emit('type-change', derivedType.value);
  }

  function handleAddElement(element: number) {
    if (element == null) return;
    if (!rows.value.some((row) => row.element === element)) {
      rows.value.push(createRow(element));
    }
    addElementKey.value = undefined;
    emitValue();
  }

  function removeRow(index: number) {
    rows.value.splice(index, 1);
    emitValue();
  }

  function moveRow(index: number, offset: number) {
    const target = index + offset;
    if (target < 0 || target >= rows.value.length) return;
    const [row] = rows.value.splice(index, 1);
    rows.value.splice(target, 0, row);
    emitValue();
  }

  /** 校验：组件属性/组件内容必须为合法 JSON 对象；返回错误信息，null 表示通过 */
  function validate(): string | null {
    if (rows.value.length === 0) {
      return '字段配置不能为空，必须包含：图标(1)、名称(2)、类别(3)';
    }
    for (const row of rows.value) {
      if (isRequiredElement(row.element) && (!row.name || !row.name.trim())) {
        return `必填组件「${elementLabel(row.element)}」的字段名不能为空`;
      }
      for (const [label, text] of [
        ['组件属性', row.attributesText],
        ['组件内容', row.contentText],
      ] as const) {
        const trimmed = (text || '').trim();
        if (!trimmed) continue;
        try {
          const parsed = JSON.parse(trimmed);
          if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
            return `「${elementLabel(row.element)}」的${label}必须是 JSON 对象`;
          }
        } catch (e) {
          return `「${elementLabel(row.element)}」的${label}不是合法 JSON`;
        }
      }
    }
    return null;
  }

  watch(
    () => props.value,
    (value) => syncFromValue(value),
    { immediate: true },
  );

  watch(
    () => [rows.value.map((row) => row.name).join('|'), rows.value.map((row) => row.isShow).join('|'), rows.value.length],
    () => emitValue(),
  );

  defineExpose({ validate });
</script>

<style lang="less" scoped>
  .field-config-editor {
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    padding: 12px 16px 16px;
    margin-bottom: 16px;

    .fce-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 10px;

      .fce-title {
        font-weight: 600;
      }

      .fce-tip {
        font-size: 12px;
        color: #999;
      }
    }

    .fce-toolbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 10px;

      .fce-type-label {
        font-size: 13px;
        color: #666;
      }
    }

    .fce-rows {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .fce-row {
      border: 1px solid #f0f0f0;
      border-radius: 4px;
      padding: 8px 10px;

      .fce-row-main {
        display: flex;
        align-items: center;
        gap: 10px;

        .fce-element-tag {
          min-width: 68px;
          font-weight: 500;
        }

        .fce-name-input {
          width: 180px;
        }

        .fce-sort-label,
        .fce-show-label {
          color: #999;
          font-size: 12px;
        }

        .fce-row-actions {
          margin-left: auto;
          white-space: nowrap;
        }
      }

      .fce-row-advanced {
        margin-top: 8px;
        display: flex;
        gap: 12px;

        .fce-json-block {
          flex: 1;

          .fce-json-label {
            font-size: 12px;
            color: #999;
            margin-bottom: 4px;
          }
        }
      }
    }

    .fce-empty {
      color: #bbb;
      font-size: 13px;
      padding: 8px 0;
    }
  }
</style>
