<template>
  <div>
    <!-- 查询区 -->
    <div class="query-bar">
      <a-select v-model:value="queryPlatform" placeholder="平台" style="width: 120px" allowClear>
        <a-select-option value="ios">iOS</a-select-option>
        <a-select-option value="android">Android</a-select-option>
      </a-select>
      <a-select v-model:value="queryStatus" placeholder="状态" style="width: 120px" allowClear>
        <a-select-option :value="0">草稿</a-select-option>
        <a-select-option :value="1">灰度中</a-select-option>
        <a-select-option :value="2">全量</a-select-option>
        <a-select-option :value="3">下线</a-select-option>
      </a-select>
      <a-button type="primary" @click="load">查询</a-button>
      <a-button type="primary" @click="openEdit(null)">新建版本</a-button>
    </div>

    <!-- 列表 -->
    <a-table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      size="middle"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'platform'">
          <a-tag :color="record.platform === 'ios' ? 'geekblue' : 'green'">
            {{ record.platform === 'ios' ? 'iOS' : 'Android' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusColor[record.status]">{{ statusText[record.status] }}</a-tag>
        </template>
        <template v-else-if="column.key === 'forceUpgrade'">
          {{ record.forceUpgrade === 1 ? `强更（< ${record.minBuild ?? '-'}）` : '仅提醒' }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a @click="openEdit(record)">编辑</a>
          <a-divider type="vertical" />
          <a-popconfirm
            v-if="record.status === 1 || record.status === 2"
            title="将此版本设为该平台当前版本？（其余发布中版本将下线）"
            @confirm="onRollback(record)"
          >
            <a>设为当前</a>
          </a-popconfirm>
          <template v-else>
            <a-popconfirm v-if="record.status === 0" title="确认灰度发布？" @confirm="onPublish(record, 1)">
              <a>灰度发布</a>
            </a-popconfirm>
            <a-divider v-if="record.status === 1" type="vertical" />
            <a-popconfirm v-if="record.status === 1" title="确认转全量？" @confirm="onPublish(record, 2)">
              <a>转全量</a>
            </a-popconfirm>
            <a-divider v-if="record.status === 1 || record.status === 2" type="vertical" />
            <a-popconfirm v-if="record.status === 1 || record.status === 2" title="确认下线该版本？" @confirm="onPublish(record, 3)">
              <a class="danger-link">下线</a>
            </a-popconfirm>
            <a-divider v-if="record.status === 0 || record.status === 3" type="vertical" />
            <a-popconfirm v-if="record.status === 0 || record.status === 3" title="确认删除？" @confirm="onDelete(record)">
              <a class="danger-link">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </template>
    </a-table>

    <!-- 新建/编辑 -->
    <a-modal
      v-model:open="editOpen"
      :title="form.id ? '编辑版本' : '新建版本'"
      :confirm-loading="saving"
      width="640px"
      @ok="onSave"
    >
      <a-form layout="vertical" style="margin-top: 12px">
        <div class="form-grid">
          <a-form-item label="平台" required>
            <a-select v-model:value="form.platform" :disabled="!!form.id">
              <a-select-option value="ios">iOS</a-select-option>
              <a-select-option value="android">Android</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="版本字符串（如 1.9.10）" required>
            <a-input v-model:value="form.version" placeholder="双平台统一" />
          </a-form-item>
          <a-form-item label="构建号 buildNo（分平台独立递增）" required>
            <a-input-number v-model:value="form.buildNo" :min="1" style="width: 100%" />
          </a-form-item>
          <a-form-item label="升级链接（商店页 / APK 直链）">
            <a-input v-model:value="form.upgradeUrl" placeholder="https://..." />
          </a-form-item>
          <a-form-item label="强更门禁 minBuild（低于此构建强更；留空不设门禁）">
            <a-input-number v-model:value="form.minBuild" :min="0" style="width: 100%" />
          </a-form-item>
          <a-form-item label="强更开关">
            <a-select v-model:value="form.forceUpgrade">
              <a-select-option :value="0">仅提醒</a-select-option>
              <a-select-option :value="1">门禁以下强更</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="灰度百分比（deviceId 稳定分桶；强更不受灰度限制）">
            <a-slider v-model:value="form.rolloutPct" :min="0" :max="100" :marks="{ 0: '0%', 50: '50%', 100: '100%' }" />
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="form.status">
              <a-select-option :value="0">草稿</a-select-option>
              <a-select-option :value="1">灰度中</a-select-option>
              <a-select-option :value="2">全量</a-select-option>
            </a-select>
          </a-form-item>
        </div>
        <a-form-item label="更新说明（弹窗展示）">
          <a-textarea v-model:value="form.changelog" :rows="3" placeholder="我们优化了体验并修复了已知问题。" />
        </a-form-item>
        <div class="form-grid">
          <a-form-item label="APK MD5（Android 直链完整性校验）">
            <a-input v-model:value="form.apkMd5" placeholder="32 位 md5" />
          </a-form-item>
          <a-form-item label="APK 大小（字节）">
            <a-input-number v-model:value="form.apkSize" :min="0" style="width: 100%" />
          </a-form-item>
        </div>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
  import { onMounted, reactive, ref } from 'vue';
  import { Modal } from 'ant-design-vue';
  import { deleteAppVersion, listAppVersion, rollbackAppVersion, saveAppVersion } from '/@/api/appversion/appversionApi';

  const statusText = ['草稿', '灰度中', '全量', '下线'];
  const statusColor = ['default', 'orange', 'green', 'red'];

  const columns = [
    { title: '平台', key: 'platform', width: 90 },
    { title: '版本', dataIndex: 'version', key: 'version', width: 100 },
    { title: '构建号', dataIndex: 'buildNo', key: 'buildNo', width: 90 },
    { title: '强更', key: 'forceUpgrade', width: 130 },
    { title: '灰度', dataIndex: 'rolloutPct', key: 'rolloutPct', width: 80, customRender: ({ text }) => `${text ?? 100}%` },
    { title: '状态', key: 'status', width: 90 },
    { title: '升级链接', dataIndex: 'upgradeUrl', key: 'upgradeUrl', ellipsis: true },
    { title: '发布时间', dataIndex: 'publishTime', key: 'publishTime', width: 160 },
    { title: '操作', key: 'action', width: 210, fixed: 'right' },
  ];

  const rows = ref<any[]>([]);
  const loading = ref(false);
  const queryPlatform = ref<string>();
  const queryStatus = ref<number>();
  const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true });

  const editOpen = ref(false);
  const saving = ref(false);
  const emptyForm = () => ({
    id: null as any,
    platform: 'ios',
    version: '',
    buildNo: undefined as any,
    minBuild: undefined as any,
    forceUpgrade: 0,
    rolloutPct: 100,
    status: 0,
    upgradeUrl: '',
    apkMd5: '',
    apkSize: undefined as any,
    changelog: '',
  });
  const form = reactive(emptyForm());

  function openEdit(record: any) {
    Object.assign(form, emptyForm(), record ?? {});
    editOpen.value = true;
  }

  async function load() {
    loading.value = true;
    try {
      const data = await listAppVersion({
        pageNo: pagination.current,
        pageSize: pagination.pageSize,
        platform: queryPlatform.value,
        status: queryStatus.value,
      });
      rows.value = data?.records ?? [];
      pagination.total = data?.total ?? 0;
    } finally {
      loading.value = false;
    }
  }

  function onTableChange(pag: any) {
    pagination.current = pag.current;
    pagination.pageSize = pag.pageSize;
    load();
  }

  async function onSave() {
    saving.value = true;
    try {
      await saveAppVersion({ ...form });
      editOpen.value = false;
      await load();
    } catch (e: any) {
      Modal.error({ title: '保存失败', content: e?.message ?? '请检查填写' });
    } finally {
      saving.value = false;
    }
  }

  async function onPublish(record: any, status: number) {
    await saveAppVersion({ ...record, status });
    await load();
  }

  async function onRollback(record: any) {
    await rollbackAppVersion({ platform: record.platform, targetId: record.id });
    await load();
  }

  async function onDelete(record: any) {
    await deleteAppVersion({ id: record.id });
    await load();
  }

  onMounted(load);
</script>

<style lang="less" scoped>
  .query-bar {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
    align-items: center;
  }
  .form-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    column-gap: 16px;
  }
  .danger-link {
    color: #ff4d4f;
  }
</style>
