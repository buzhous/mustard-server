<template>
  <div>
    <a-alert
      class="page-tip"
      type="info"
      show-icon
      message="系统分类为 App、AI 建档、后台共用的全局分类体系；名称唯一，停用不物理删除（已实例化的用户分类仍指向该行）；编辑即时生效，主题管理与 App 系统分类下拉同步可用。"
    />
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button preIcon="ant-design:plus-outlined" type="primary" @click="handleAdd" style="margin-right: 5px">新增分类</a-button>
        <a-button preIcon="ant-design:reload-outlined" @click="reload">刷新</a-button>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" />
      </template>
    </BasicTable>
    <CategoryDrawer @register="registerDrawer" @success="reload" />
  </div>
</template>
<script lang="ts" setup name="base-category">
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { list, changeStatus } from '/@/api/base/categoryApi';
  import { columns, searchFormSchema } from './data';
  import CategoryDrawer from './CategoryDrawer.vue';

  const [registerDrawer, { openDrawer }] = useDrawer();
  const { tableContext } = useListPage({
    designScope: 'base-category-template',
    tableProps: {
      title: '系统分类列表',
      api: list,
      columns: columns,
      actionColumn: {
        width: 140,
      },
      formConfig: {
        labelWidth: 90,
        schemas: searchFormSchema,
      },
    },
  });

  const [registerTable, { reload }] = tableContext;

  function getActions(record) {
    return [
      {
        label: '编辑',
        onClick: handleEdit.bind(null, record),
      },
      {
        // 停用不物理删除（已实例化的用户分类保留聚合关系）
        label: record.status == 1 ? '停用' : '启用',
        popConfirm: {
          title: record.status == 1 ? '确认停用该分类？已实例化的用户分类仍指向该行' : '确认启用该分类？',
          confirm: handleToggle.bind(null, record),
        },
      },
    ];
  }

  function handleAdd() {
    openDrawer(true, {
      isUpdate: false,
    });
  }

  function handleEdit(record) {
    openDrawer(true, {
      record,
      isUpdate: true,
    });
  }

  async function handleToggle(record) {
    await changeStatus({ id: record.id, status: record.status == 1 ? 0 : 1 }, reload);
  }
</script>
<style lang="less" scoped>
  .page-tip {
    margin-bottom: 8px;
  }
</style>
