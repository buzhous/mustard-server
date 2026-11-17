<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="用户仓库详情" :showOkBtn="false" :width="860">
    <a-spin :spinning="loading">
      <a-descriptions title="用户信息" :column="3" size="small" bordered>
        <a-descriptions-item label="用户Id">{{ detailData.user?.id || '-' }}</a-descriptions-item>
        <a-descriptions-item label="手机号">{{ detailData.user?.phone || '-' }}</a-descriptions-item>
        <a-descriptions-item label="昵称">{{ detailData.user?.nickname || '-' }}</a-descriptions-item>
      </a-descriptions>

      <a-descriptions title="虚拟仓（固定 1 个，不可删除）" :column="3" size="small" bordered class="mt-4">
        <a-descriptions-item label="虚拟仓编号">{{ detailData.virtual?.stationNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="仓库名称">{{ detailData.virtual?.customName || '虚拟仓' }}</a-descriptions-item>
        <a-descriptions-item label="位置说明">{{ detailData.virtual?.locationDesc || '-' }}</a-descriptions-item>
      </a-descriptions>

      <div class="mt-4 font-bold">自有真实仓（普通用户最多 1 个，VIP 最多 3 个）</div>
      <a-table :columns="storageColumns" :data-source="detailData.storages || []" :pagination="false" rowKey="id" size="small" class="mt-2">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'storageType'">{{ record.storageType === 'REAL' ? '自有真实仓' : '虚拟仓' }}</template>
          <template v-else-if="column.dataIndex === 'isDefault'">{{ record.isDefault == 1 ? '默认' : '-' }}</template>
        </template>
      </a-table>

      <div class="mt-4 font-bold">官方绑定仓（普通用户最多绑定 1 个，VIP 最多 3 个）</div>
      <a-table :columns="bindColumns" :data-source="detailData.binds || []" :pagination="false" rowKey="id" size="small" class="mt-2" />
    </a-spin>
  </BasicModal>
</template>

<script lang="ts" name="warehouse-user-storage-detail" setup>
  import { ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { detail } from '/@/api/warehouse/userWarehouseApi';
  import { bindColumns } from './data';

  const loading = ref(false);
  const detailData = ref<any>({});

  const storageColumns = [
    { title: '仓库名称', dataIndex: 'name', width: 160 },
    { title: '仓库类型', dataIndex: 'storageType', width: 120 },
    { title: '默认仓', dataIndex: 'isDefault', width: 90 },
    { title: '创建时间', dataIndex: 'createTime', width: 160 },
  ];

  const [registerModal] = useModalInner(async (data) => {
    detailData.value = {};
    const userKey = data?.record?.userId || data?.userKey;
    if (!userKey) return;
    loading.value = true;
    try {
      detailData.value = await detail({ userKey });
    } finally {
      loading.value = false;
    }
  });
</script>
