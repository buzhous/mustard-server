<template>
  <div>
    <a-alert
      class="page-tip"
      type="info"
      show-icon
      message="字段字典为 App、服务端、后台三端共用的唯一字段数据源；内置 34 个组件，可扩展新增（编号自动顺延）。保存后由独立基础服务即时刷新 Redis 缓存、版本 +1 并续期（365 天），App 登录/初始化比对版本后按端同步。已录入字段一律不可删除，仅支持停用/启用。"
    />
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button preIcon="ant-design:plus-outlined" type="primary" @click="handleAdd" style="margin-right: 5px">新增字段</a-button>
        <a-button preIcon="ant-design:reload-outlined" @click="reload">刷新</a-button>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" />
      </template>
    </BasicTable>
    <FieldDrawer @register="registerDrawer" @success="reload" />
  </div>
</template>
<script lang="ts" setup name="base-field">
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { list, changeStatus } from '/@/api/base/fieldElementApi';
  import { columns, searchFormSchema } from './data';
  import FieldDrawer from './FieldDrawer.vue';

  const [registerDrawer, { openDrawer }] = useDrawer();
  const { tableContext } = useListPage({
    designScope: 'base-field-template',
    tableProps: {
      title: '字段字典列表',
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
        // 已录入字段不可删除，仅支持停用/启用
        label: record.status == 1 ? '停用' : '启用',
        popConfirm: {
          title: record.status == 1 ? '确认停用该字段？停用后 App 初始化不再下发' : '确认启用该字段？',
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
