<template>
  <div>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-dropdown v-if="selectedRowKeys.length > 0">
          <template #overlay>
            <a-menu>
              <a-menu-item key="1" @click="batchHandleOffShelf">
                <Icon icon="ant-design:stop-outlined"></Icon>
                批量下架
              </a-menu-item>
              <a-menu-item key="2" @click="openAssignModal">
                <Icon icon="ant-design:user-add-outlined"></Icon>
                批量指定拾取用户
              </a-menu-item>
              <a-menu-item key="3" @click="batchHandleDelete">
                <Icon icon="ant-design:delete-outlined"></Icon>
                批量删除
              </a-menu-item>
            </a-menu>
          </template>
          <a-button>
            批量操作
            <Icon icon="mdi:chevron-down"></Icon>
          </a-button>
        </a-dropdown>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" />
      </template>
    </BasicTable>

    <a-modal
      v-model:open="assignVisible"
      title="批量指定拾取用户"
      :confirm-loading="assignLoading"
      @ok="handleAssign"
    >
      <a-input v-model:value="assignKey" placeholder="输入手机号 / 用户名 / 用户Id" allow-clear />
      <p class="assign-tip">
        已选 {{ selectedRowKeys.length }} 件物品，指派后仅该用户在殷墟广场可见并拾取（仅"待拾取"状态的物品会被指派）。
      </p>
    </a-modal>
  </div>
</template>

<script lang="ts" name="ruins-admin" setup>
  import { ref } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { list, deleteRuins, deleteBatch, offShelfBatch, assignPickupBatch } from '/@/api/ruins/ruinsAdminApi';
  import { columns, searchFormSchema } from './data';

  const { createMessage, createConfirm } = useMessage();

  // 列表页面公共参数、方法
  const { tableContext } = useListPage({
    designScope: 'ruins-admin-template',
    tableProps: {
      title: '殷墟物品列表',
      api: list,
      columns: columns,
      actionColumn: {
        width: 120,
      },
      formConfig: {
        labelWidth: 90,
        schemas: searchFormSchema,
      },
    },
  });

  const [registerTable, { reload }, { rowSelection, selectedRowKeys }] = tableContext;

  const assignVisible = ref(false);
  const assignLoading = ref(false);
  const assignKey = ref('');

  /**
   * 操作列定义
   * @param record
   */
  function getActions(record) {
    return [
      {
        label: '删除',
        popConfirm: {
          title: '是否确认删除',
          confirm: handleDelete.bind(null, record),
        },
      },
    ];
  }

  /**
   * 单条删除
   */
  async function handleDelete(record) {
    await deleteRuins({ id: record.id }, reload);
  }

  /**
   * 批量删除
   */
  async function batchHandleDelete() {
    await deleteBatch({ ids: selectedRowKeys.value }, () => {
      selectedRowKeys.value = [];
      reload();
    });
  }

  /**
   * 批量下架（仅待拾取状态可下架）
   */
  function batchHandleOffShelf() {
    createConfirm({
      iconType: 'warning',
      title: '确认下架',
      content: '下架后物品将从殷墟广场隐藏且不可拾取，是否继续？',
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const res = await offShelfBatch({ ids: selectedRowKeys.value });
        createMessage.success(res?.message || '批量下架成功');
        selectedRowKeys.value = [];
        reload();
      },
    });
  }

  /**
   * 打开指派弹窗
   */
  function openAssignModal() {
    assignKey.value = '';
    assignVisible.value = true;
  }

  /**
   * 批量指定拾取用户
   */
  async function handleAssign() {
    if (!assignKey.value.trim()) {
      createMessage.warning('请填写要指派的用户（手机号/用户名/用户Id）');
      return;
    }
    assignLoading.value = true;
    try {
      const res = await assignPickupBatch({ ids: selectedRowKeys.value, userKey: assignKey.value.trim() });
      createMessage.success(res?.message || '指派成功');
      assignVisible.value = false;
      selectedRowKeys.value = [];
      reload();
    } finally {
      assignLoading.value = false;
    }
  }
</script>

<style lang="less" scoped>
  .assign-tip {
    margin-top: 12px;
    color: #999;
    font-size: 12px;
    line-height: 1.6;
  }
</style>
