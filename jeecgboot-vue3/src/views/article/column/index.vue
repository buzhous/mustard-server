<template>
  <div>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" pre-icon="ant-design:plus-outlined" @click="openAdd">新增栏目</a-button>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" />
      </template>
    </BasicTable>

    <a-modal
      v-model:open="editVisible"
      :title="editForm.id ? '编辑栏目' : '新增栏目'"
      width="520px"
      :confirm-loading="editLoading"
      ok-text="保存"
      @ok="handleSave"
    >
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="栏目名称" required>
          <a-input v-model:value="editForm.columnName" placeholder="请输入栏目名称（50 字内）" :maxlength="50" />
        </a-form-item>
        <a-form-item label="栏目编码" required>
          <a-input v-model:value="editForm.columnCode" placeholder="字母开头，仅字母/数字/下划线（如 agreement）" :maxlength="50" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="editForm.sortNo" :min="0" :precision="0" style="width: 140px" />
        </a-form-item>
        <a-form-item label="状态">
          <a-radio-group v-model:value="editForm.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">停用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="editForm.remark" placeholder="备注（选填）" :rows="3" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" name="article-column" setup>
  import { ref } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { columnList, addColumn, editColumn, deleteColumn } from '/@/api/article/articleAdminApi';
  import { columns } from './data';

  const { createMessage } = useMessage();

  // 栏目列表不分页，走 useListPage 标准 API 再由后端全量返回
  const { tableContext } = useListPage({
    designScope: 'article-column-template',
    tableProps: {
      title: '文章栏目列表',
      api: columnList,
      columns: columns,
      pagination: false,
      actionColumn: {
        width: 140,
      },
    },
  });

  const [registerTable, { reload }] = tableContext;

  const editVisible = ref(false);
  const editLoading = ref(false);
  const editForm = ref<any>(getDefaultForm());

  function getDefaultForm() {
    return {
      id: '',
      columnName: '',
      columnCode: '',
      sortNo: 0,
      status: 1,
      remark: '',
    };
  }

  /**
   * 操作列定义
   * @param record
   */
  function getActions(record) {
    return [
      {
        label: '编辑',
        onClick: () => openEdit(record),
      },
      {
        label: '删除',
        popConfirm: {
          title: '栏目下存在文章时不可删除，是否继续删除？',
          confirm: handleDelete.bind(null, record),
        },
      },
    ];
  }

  function openAdd() {
    editForm.value = getDefaultForm();
    editVisible.value = true;
  }

  function openEdit(record) {
    editForm.value = {
      id: record.id,
      columnName: record.columnName,
      columnCode: record.columnCode,
      sortNo: record.sortNo ?? 0,
      status: record.status ?? 1,
      remark: record.remark || '',
    };
    editVisible.value = true;
  }

  async function handleSave() {
    const f = editForm.value;
    if (!f.columnName?.trim()) {
      createMessage.warning('请填写栏目名称');
      return;
    }
    if (!f.columnCode?.trim()) {
      createMessage.warning('请填写栏目编码');
      return;
    }
    editLoading.value = true;
    try {
      const res = f.id ? await editColumn(f) : await addColumn(f);
      createMessage.success(res?.message || '保存成功');
      editVisible.value = false;
      reload();
    } finally {
      editLoading.value = false;
    }
  }

  async function handleDelete(record) {
    await deleteColumn({ id: record.id }, reload);
  }
</script>
