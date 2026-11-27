<template>
  <div class="ailing-tune" style="padding: 16px">
    <a-card :bordered="false" :body-style="{ paddingTop: '8px' }">
      <a-tabs v-model:activeKey="activeTab">
        <!-- 区块① 兜底链观测 -->
        <a-tab-pane key="observe" tab="兜底链观测">
          <a-spin :spinning="observeLoading">
            <a-radio-group v-model:value="observeDays" option-type="button" :options="daysOptions" @change="reloadObserve" />
            <a-row :gutter="12" style="margin-top: 12px">
              <a-col :span="4">
                <a-card size="small"><a-statistic title="总查询数" :value="stats.totalQueries ?? 0" /></a-card>
              </a-col>
              <a-col :span="5">
                <a-card size="small"><a-statistic title="缓存命中率" :value="stats.cacheHitRate ?? 0" suffix="%" /></a-card>
              </a-col>
              <a-col :span="5">
                <a-card size="small"><a-statistic title="LLM 兜底占比" :value="stats.llmPickShare ?? 0" suffix="%" /></a-card>
              </a-col>
              <a-col :span="5">
                <a-card size="small"><a-statistic title="平均耗时" :value="stats.avgTotalMs ?? 0" suffix="ms" /></a-card>
              </a-col>
              <a-col :span="5">
                <a-card size="small"><a-statistic title="P95 耗时" :value="stats.p95TotalMs ?? 0" suffix="ms" /></a-card>
              </a-col>
            </a-row>

            <BasicTable
              title="各环节命中率与平均耗时"
              :dataSource="stats.stages || []"
              :columns="stageColumns"
              rowKey="stage"
              :pagination="false"
              :showTableSetting="false"
              :canResize="false"
              style="margin-top: 16px"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'stage'">
                  <a-tag :color="stageColorMap[record.stage] || 'default'">{{ stageMap[record.stage] || record.stage }}</a-tag>
                </template>
                <template v-else-if="column.key === 'hitRate'">
                  {{ stageHitRate(record) }}
                </template>
                <template v-else-if="column.key === 'avgMs'">
                  {{ record.avgMs }} ms
                </template>
              </template>
            </BasicTable>

            <BasicTable
              title="低命中样本（source_type=2，最近 200 条）"
              :dataSource="misses"
              :columns="missColumns"
              rowKey="id"
              :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }"
              :showTableSetting="false"
              :canResize="false"
              style="margin-top: 16px"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'username'">
                  {{ record.username || record.uid || '-' }}
                </template>
                <template v-else-if="column.key === 'sample_status'">
                  <a-tag :color="sampleStatusColorMap[String(record.sample_status)] || 'default'">
                    {{ sampleStatusMap[String(record.sample_status)] ?? record.sample_status }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-button type="link" size="small" v-auth="'ailing:tune:edit'" @click="openMissAlias(record)">补词表</a-button>
                  <a-button type="link" size="small" v-auth="'ailing:tune:edit'" @click="exportBaseline(record)">导出基准用例</a-button>
                  <a-popconfirm title="确认忽略该样本？" @confirm="ignoreMiss(record)">
                    <a-button type="link" size="small" danger v-auth="'ailing:tune:edit'">忽略</a-button>
                  </a-popconfirm>
                </template>
              </template>
            </BasicTable>
          </a-spin>
        </a-tab-pane>

        <!-- 区块② 提示词编辑 -->
        <a-tab-pane key="prompt" tab="提示词编辑">
          <a-spin :spinning="configLoading">
            <a-tabs v-model:activeKey="activeSkill">
              <a-tab-pane key="filing" tab="建档技能包（filing）" />
              <a-tab-pane key="query" tab="查询技能包（query）" />
            </a-tabs>
            <a-space wrap style="margin-bottom: 8px">
              <a-tag :color="skillSourceTag(skillView).color" style="font-size: 13px">{{ skillSourceTag(skillView).text }}</a-tag>
              <span style="color: #999">必需占位符（{{ (skillView.requiredVars || []).length }} 个）：</span>
              <a-tag v-for="v in skillView.requiredVars || []" :key="v" color="geekblue">{{ '{' + '{' + v + '}' + '}' }}</a-tag>
            </a-space>
            <a-alert
              v-if="promptError[activeSkill]"
              type="error"
              :message="promptError[activeSkill]"
              show-icon
              closable
              style="margin-bottom: 8px"
              @close="promptError[activeSkill] = ''"
            />
            <a-textarea
              v-model:value="promptContent[activeSkill]"
              :rows="18"
              class="prompt-editor"
              placeholder="输入提示词内容（DB 值运行时同样走双花括号变量填充，保存前必须保留全部必需占位符）"
            />
            <a-space style="margin-top: 12px">
              <a-button type="primary" v-auth="'ailing:tune:edit'" :loading="promptSaving" @click="onSavePrompt">保存（生成新版本）</a-button>
              <a-button v-auth="'ailing:tune:edit'" danger @click="onRollbackPrompt">回退内置</a-button>
            </a-space>

            <BasicTable
              title="版本历史（时间 / 版本 / 操作人，恢复 = 内容写入为新生效版本）"
              :dataSource="skillHistory"
              :columns="historyColumns"
              rowKey="version"
              :pagination="false"
              :showTableSetting="false"
              :canResize="false"
              style="margin-top: 16px"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'version'">
                  v{{ record.version }}
                </template>
                <template v-else-if="column.key === 'rowState'">
                  <a-tag :color="historyRowState(record).color">{{ historyRowState(record).text }}</a-tag>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-button
                    type="link"
                    size="small"
                    v-auth="'ailing:tune:edit'"
                    :disabled="Number(record.active) === 1 && Number(record.enabled) === 1"
                    @click="confirmRestore(record)"
                  >
                    恢复
                  </a-button>
                </template>
              </template>
            </BasicTable>
          </a-spin>
        </a-tab-pane>

        <!-- 区块③ 模型与检索参数 -->
        <a-tab-pane key="params" tab="模型与检索参数">
          <a-spin :spinning="configLoading">
            <a-alert
              type="info"
              show-icon
              message="保存后配置即刻下发全节点生效；数值范围：temperature 0–2、maxAsks 0–5（整数）、缓存 TTL 5–600 秒（整数）"
              style="margin-bottom: 16px"
            />
            <a-form layout="vertical" style="max-width: 680px">
              <a-divider orientation="left" plain>模型参数</a-divider>
              <a-row :gutter="16">
                <a-col :span="8">
                  <a-form-item label="对话模型 chat_model">
                    <a-input v-model:value="paramForm.chatModel" placeholder="如 qwen-max" />
                    <span class="param-source">来源：{{ paramSource(modelView.chatModel) }}</span>
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="温度 temperature">
                    <a-input-number v-model:value="paramForm.temperature" :min="0" :max="2" :step="0.1" style="width: 100%" />
                    <span class="param-source">来源：{{ paramSource(modelView.temperature) }}</span>
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="最大追问轮数 max_asks">
                    <a-input-number v-model:value="paramForm.maxAsks" :min="0" :max="5" :precision="0" style="width: 100%" />
                    <span class="param-source">来源：{{ paramSource(modelView.maxAsks) }}</span>
                  </a-form-item>
                </a-col>
              </a-row>

              <a-divider orientation="left" plain>检索参数</a-divider>
              <a-row :gutter="16">
                <a-col :span="6">
                  <a-form-item label="别名检索 alias_enabled">
                    <a-switch v-model:checked="paramForm.aliasEnabled" />
                    <span class="param-source">来源：{{ paramSource(searchView.aliasEnabled) }}</span>
                  </a-form-item>
                </a-col>
                <a-col :span="6">
                  <a-form-item label="索引检索 index_enabled">
                    <a-switch v-model:checked="paramForm.indexEnabled" />
                    <span class="param-source">来源：{{ paramSource(searchView.indexEnabled) }}</span>
                  </a-form-item>
                </a-col>
                <a-col :span="6">
                  <a-form-item label="意图缓存 cache_enabled">
                    <a-switch v-model:checked="paramForm.cacheEnabled" />
                    <span class="param-source">来源：{{ paramSource(searchView.cacheEnabled) }}</span>
                  </a-form-item>
                </a-col>
                <a-col :span="6">
                  <a-form-item label="缓存 TTL cache_ttl_seconds">
                    <a-input-number v-model:value="paramForm.cacheTtlSeconds" :min="5" :max="600" :precision="0" style="width: 100%" />
                    <span class="param-source">来源：{{ paramSource(searchView.cacheTtlSeconds) }}</span>
                  </a-form-item>
                </a-col>
              </a-row>

              <a-button type="primary" v-auth="'ailing:tune:edit'" :loading="paramsSaving" @click="onSaveParams">保存参数</a-button>
            </a-form>
          </a-spin>
        </a-tab-pane>

        <!-- 区块④ 词表维护 -->
        <a-tab-pane key="alias" tab="词表维护">
          <BasicTable @register="registerAliasTable">
            <template #tableTitle>
              <a-button type="primary" v-auth="'ailing:tune:edit'" @click="openAliasModal()">新增词表</a-button>
            </template>
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'alias_type'">
                <a-tag :color="aliasTypeColorMap[String(record.alias_type)] || 'default'">
                  {{ aliasTypeMap[String(record.alias_type)] ?? record.alias_type }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'enabled'">
                <a-switch v-auth="'ailing:tune:edit'" :checked="Number(record.enabled) === 1" @change="(checked: any) => onAliasEnabled(record, checked)" />
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" size="small" v-auth="'ailing:tune:edit'" @click="openAliasModal(record)">编辑</a-button>
                <a-popconfirm title="确认删除该词条（物理删除）？" @confirm="onDeleteAlias(record)">
                  <a-button type="link" size="small" danger v-auth="'ailing:tune:edit'">删除</a-button>
                </a-popconfirm>
              </template>
            </template>
          </BasicTable>

          <a-modal
            v-model:open="aliasModal.open"
            :title="aliasModal.id ? '编辑词表' : '新增词表'"
            :confirmLoading="aliasSaving"
            :maskClosable="false"
            @ok="submitAlias"
          >
            <a-form layout="vertical">
              <a-form-item label="别名（用户查询可能出现的说法）" required>
                <a-input v-model:value="aliasModal.alias" :disabled="!!aliasModal.id" placeholder="如：肚脐贴" />
              </a-form-item>
              <a-form-item label="标准词（库内物品标准词）" required>
                <a-input v-model:value="aliasModal.standard" placeholder="如：神阙穴贴敷" />
              </a-form-item>
              <a-form-item label="词型">
                <a-select v-model:value="aliasModal.aliasType" :options="aliasTypeOptions" />
              </a-form-item>
              <a-form-item label="启用">
                <a-switch v-model:checked="aliasModal.enabled" />
              </a-form-item>
            </a-form>
            <div style="color: #999">提示：编辑时别名不可修改；如需改别名请删除后重建（写后自动清词表缓存）。</div>
          </a-modal>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 低命中样本 → 补词表弹窗 -->
    <a-modal
      v-model:open="missAliasModal.open"
      title="补词表（低命中样本处置）"
      :confirmLoading="missAliasSaving"
      :maskClosable="false"
      @ok="submitMissAlias"
    >
      <a-form layout="vertical">
        <a-form-item label="查询词（写入词表别名）">
          <a-input :value="missAliasModal.alias" disabled />
        </a-form-item>
        <a-form-item label="标准词（必填）" required>
          <a-input v-model:value="missAliasModal.standard" placeholder="库内物品标准词" />
        </a-form-item>
        <a-form-item label="词型">
          <a-select v-model:value="missAliasModal.aliasType" :options="aliasTypeOptions" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup name="ailing-tune">
  import { computed, onMounted, reactive, ref } from 'vue';
  import { BasicTable, useTable } from '/@/components/Table';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getAliasList, deleteAlias, getObserveStats, getObserveMisses, getTuneConfig, handleFeedback, restorePrompt, rollbackPrompt, saveAlias, saveParams, savePrompt } from './api';
  import {
    FILING_PROMPT_KEY,
    QUERY_PROMPT_KEY,
    aliasColumns,
    aliasSearchSchema,
    aliasTypeColorMap,
    aliasTypeMap,
    aliasTypeOptions,
    daysOptions,
    downloadJson,
    historyColumns,
    historyRowState,
    missColumns,
    sampleStatusColorMap,
    sampleStatusMap,
    skillSourceTag,
    stageColorMap,
    stageColumns,
    stageHitRate,
    stageMap,
  } from './data';

  const { createMessage, createConfirm } = useMessage();

  const activeTab = ref('observe');
  const activeSkill = ref<'filing' | 'query'>('filing');
  const skillLabel = computed(() => (activeSkill.value === 'filing' ? '建档' : '查询'));

  // ================================ 全量配置视图 ================================
  const configLoading = ref(false);
  const config = ref<Recordable>({});

  async function loadConfig() {
    configLoading.value = true;
    try {
      const res: any = await getTuneConfig();
      config.value = res || {};
      initPromptContent();
      initParamForm();
    } finally {
      configLoading.value = false;
    }
  }

  const skillView = computed<Recordable>(() => (activeSkill.value === 'filing' ? config.value.filing : config.value.query) || {});
  const modelView = computed<Recordable>(() => config.value.model || {});
  const searchView = computed<Recordable>(() => config.value.search || {});
  const skillHistory = computed<Recordable[]>(() =>
    (config.value.history || []).filter((h: Recordable) => h.key === (activeSkill.value === 'filing' ? FILING_PROMPT_KEY : QUERY_PROMPT_KEY))
  );

  /** 参数项来源文案：DB 配置 / 内置默认（值） */
  function paramSource(item?: Recordable): string {
    if (!item) return '-';
    return item.source === 'db' ? 'DB 配置' : `内置默认（${item.builtin === undefined ? '-' : item.builtin}）`;
  }

  // ================================ 区块② 提示词编辑 ================================
  const promptContent = reactive<Record<'filing' | 'query', string>>({ filing: '', query: '' });
  const promptError = reactive<Record<'filing' | 'query', string>>({ filing: '', query: '' });
  const promptSaving = ref(false);

  function initPromptContent() {
    for (const s of ['filing', 'query'] as const) {
      const v: Recordable = config.value[s] || {};
      promptContent[s] = v.source === 'db' && v.dbValue ? String(v.dbValue) : String(v.builtin ?? '');
    }
  }

  async function onSavePrompt() {
    const skill = activeSkill.value;
    if (!promptContent[skill]?.trim()) {
      promptError[skill] = '提示词内容不能为空';
      return;
    }
    promptSaving.value = true;
    try {
      await savePrompt({ skill, content: promptContent[skill] });
      promptError[skill] = '';
      createMessage.success('保存成功，已生成新版本并即时生效');
      await loadConfig();
    } catch (e: any) {
      // 服务端拒绝（缺占位符/缺分段标记等）：errorMessageMode=none，由页面内联展示 message
      promptError[skill] = e?.message || '保存失败';
    } finally {
      promptSaving.value = false;
    }
  }

  function onRollbackPrompt() {
    const skill = activeSkill.value;
    createConfirm({
      iconType: 'warning',
      title: '回退内置提示词',
      content: `确认将「${skillLabel.value}」技能包回退到内置模板？该技能包 DB 配置将全部停用（历史保留，可从版本历史恢复）。`,
      onOk: async () => {
        await rollbackPrompt(skill);
        createMessage.success('已回退内置，即刻生效');
        await loadConfig();
      },
    });
  }

  function confirmRestore(row: Recordable) {
    createConfirm({
      iconType: 'info',
      title: '恢复历史版本',
      content: `确认将「${skillLabel.value}」技能包恢复到 v${row.version}？内容将写入为新生效版本（旧行保留，审计可追溯）。`,
      onOk: async () => {
        await restorePrompt({ skill: activeSkill.value, version: Number(row.version) });
        createMessage.success(`已恢复到 v${row.version} 并生效`);
        await loadConfig();
      },
    });
  }

  // ================================ 区块③ 模型与检索参数 ================================
  const paramForm = reactive({ chatModel: '', temperature: 0, maxAsks: 3, aliasEnabled: true, indexEnabled: true, cacheEnabled: true, cacheTtlSeconds: 60 });

  function initParamForm() {
    const m: Recordable = config.value.model || {};
    const s: Recordable = config.value.search || {};
    paramForm.chatModel = m.chatModel?.effective ?? '';
    paramForm.temperature = m.temperature?.effective ?? 0;
    paramForm.maxAsks = m.maxAsks?.effective ?? 3;
    paramForm.aliasEnabled = !!s.aliasEnabled?.effective;
    paramForm.indexEnabled = !!s.indexEnabled?.effective;
    paramForm.cacheEnabled = !!s.cacheEnabled?.effective;
    paramForm.cacheTtlSeconds = s.cacheTtlSeconds?.effective ?? 60;
  }

  const paramsSaving = ref(false);

  async function onSaveParams() {
    paramsSaving.value = true;
    try {
      await saveParams({
        model: { chatModel: String(paramForm.chatModel ?? '').trim(), temperature: paramForm.temperature, maxAsks: paramForm.maxAsks },
        search: {
          aliasEnabled: paramForm.aliasEnabled,
          indexEnabled: paramForm.indexEnabled,
          cacheEnabled: paramForm.cacheEnabled,
          cacheTtlSeconds: paramForm.cacheTtlSeconds,
        },
      });
      createMessage.success('保存成功，配置已下发并即时生效');
      await loadConfig();
    } catch {
      // 错误提示已由 axios 拦截器弹出
    } finally {
      paramsSaving.value = false;
    }
  }

  // ================================ 区块① 兜底链观测 ================================
  const observeDays = ref<number>(7);
  const observeLoading = ref(false);
  const stats = ref<Recordable>({});
  const misses = ref<Recordable[]>([]);

  async function reloadObserve() {
    observeLoading.value = true;
    try {
      const [s, m] = await Promise.all([getObserveStats(observeDays.value), getObserveMisses()]);
      stats.value = (s as Recordable) || {};
      misses.value = (m as Recordable[]) || [];
    } finally {
      observeLoading.value = false;
    }
  }

  const missAliasModal = reactive({ open: false, id: '', alias: '', standard: '', aliasType: 1 });
  const missAliasSaving = ref(false);

  function openMissAlias(row: Recordable) {
    missAliasModal.id = String(row.id ?? '');
    missAliasModal.alias = String(row.ref_content ?? '');
    missAliasModal.standard = '';
    missAliasModal.aliasType = 1;
    missAliasModal.open = true;
  }

  async function submitMissAlias() {
    if (!missAliasModal.standard.trim()) {
      createMessage.warning('请填写标准词');
      return;
    }
    missAliasSaving.value = true;
    try {
      await handleFeedback({ id: missAliasModal.id, action: 'alias', standard: missAliasModal.standard.trim(), aliasType: missAliasModal.aliasType });
      createMessage.success('已补词表并清词表缓存，可用原查询词复查命中');
      missAliasModal.open = false;
      await reloadObserve();
    } catch {
      // 错误提示已由 axios 拦截器弹出
    } finally {
      missAliasSaving.value = false;
    }
  }

  async function ignoreMiss(row: Recordable) {
    try {
      await handleFeedback({ id: String(row.id ?? ''), action: 'ignore' });
      createMessage.success('已忽略该样本');
      await reloadObserve();
    } catch {
      // 错误提示已由 axios 拦截器弹出
    }
  }

  async function exportBaseline(row: Recordable) {
    try {
      const res: any = await handleFeedback({ id: String(row.id ?? ''), action: 'baseline' });
      downloadJson(res?.filename || `case-obs-${row.id}.json`, res?.case);
      createMessage.success('基准用例已导出，请人工并入 benchmark 目录');
      await reloadObserve();
    } catch {
      // 错误提示已由 axios 拦截器弹出
    }
  }

  // ================================ 区块④ 词表维护 ================================
  const [registerAliasTable, { reload: reloadAlias }] = useTable({
    title: '关键词词表（写后自动清 ail:alias:cache，App 侧 TTL 60s 内自然过期兜底）',
    api: getAliasList,
    rowKey: 'id',
    columns: aliasColumns,
    canResize: false,
    showTableSetting: true,
    formConfig: { schemas: aliasSearchSchema, autoSubmitOnEnter: true },
  });

  const aliasModal = reactive({ open: false, id: '', alias: '', standard: '', aliasType: 1, enabled: true });
  const aliasSaving = ref(false);

  function openAliasModal(row?: Recordable) {
    aliasModal.id = row ? String(row.id ?? '') : '';
    aliasModal.alias = row ? String(row.alias ?? '') : '';
    aliasModal.standard = row ? String(row.standard ?? '') : '';
    aliasModal.aliasType = row ? Number(row.alias_type) || 1 : 1;
    aliasModal.enabled = row ? Number(row.enabled) === 1 : true;
    aliasModal.open = true;
  }

  async function submitAlias() {
    if (!aliasModal.alias.trim() || !aliasModal.standard.trim()) {
      createMessage.warning('别名与标准词均不能为空');
      return;
    }
    aliasSaving.value = true;
    try {
      await saveAlias({
        id: aliasModal.id || undefined,
        alias: aliasModal.alias.trim(),
        standard: aliasModal.standard.trim(),
        aliasType: aliasModal.aliasType,
        enabled: aliasModal.enabled ? 1 : 0,
      });
      createMessage.success(aliasModal.id ? '词表已更新' : '词表已新增');
      aliasModal.open = false;
      await reloadAlias();
    } catch {
      // 错误提示已由 axios 拦截器弹出（如 uk_alias 冲突）
    } finally {
      aliasSaving.value = false;
    }
  }

  async function onAliasEnabled(row: Recordable, checked: any) {
    row.enabled = checked ? 1 : 0;
    try {
      await saveAlias({
        id: String(row.id ?? ''),
        alias: String(row.alias ?? ''),
        standard: String(row.standard ?? ''),
        aliasType: Number(row.alias_type) || 1,
        enabled: checked ? 1 : 0,
      });
      createMessage.success(checked ? '已启用' : '已停用');
    } catch {
      await reloadAlias(); // 失败回滚显示态
    }
  }

  async function onDeleteAlias(row: Recordable) {
    try {
      await deleteAlias(String(row.id ?? ''));
      createMessage.success('词条已删除');
      await reloadAlias();
    } catch {
      // 错误提示已由 axios 拦截器弹出
    }
  }

  onMounted(async () => {
    loadConfig();
    reloadObserve();
  });
</script>

<style lang="less" scoped>
  .ailing-tune {
    background: #fff;
    border-radius: 8px;
  }

  .prompt-editor {
    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
    font-size: 12px;
  }

  .param-source {
    display: block;
    margin-top: 4px;
    font-size: 12px;
    color: #999;
  }
</style>
