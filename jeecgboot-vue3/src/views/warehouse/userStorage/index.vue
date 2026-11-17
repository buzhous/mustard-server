<template>
  <div>
    <BasicTable @register="registerTable">
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" />
      </template>
    </BasicTable>
    <UserDetailModal @register="registerDetailModal" />
  </div>
</template>
<script lang="ts" name="warehouse-user-storage" setup>
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { page } from '/@/api/warehouse/userWarehouseApi';
  import { columns, searchFormSchema } from './data';
  import UserDetailModal from './UserDetailModal.vue';

  const [registerDetailModal, { openModal }] = useModal();

  // 列表页面公共参数、方法
  const { tableContext } = useListPage({
    designScope: 'warehouse-user-storage-template',
    tableProps: {
      title: '用户自有仓列表',
      api: page,
      columns: columns,
      actionColumn: {
        width: 120,
      },
      formConfig: {
        labelWidth: 80,
        schemas: searchFormSchema,
      },
    },
  });

  const [registerTable, { reload }] = tableContext;

  /**
   * 操作列定义
   * @param record
   */
  function getActions(record) {
    return [
      {
        label: '仓库详情',
        onClick: handleDetail.bind(null, record),
      },
    ];
  }

  /**
   * 查看用户全部仓库（虚拟仓 + 自有仓 + 官方绑定仓）
   */
  function handleDetail(record) {
    openModal(true, {
      record,
      isUpdate: false,
    });
  }
</script>
