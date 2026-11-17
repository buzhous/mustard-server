<template>
  <div>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-button type="primary" pre-icon="ant-design:plus-outlined" @click="openAdd">新增文章</a-button>
        <a-dropdown v-if="selectedRowKeys.length > 0" style="margin-left: 8px">
          <template #overlay>
            <a-menu>
              <a-menu-item key="1" @click="batchHandleDelete">
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
      v-model:open="editVisible"
      :title="editForm.id ? '编辑文章' : '新增文章'"
      width="860px"
      :confirm-loading="editLoading"
      ok-text="保存"
      @ok="handleSave"
    >
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 19 }">
        <a-form-item label="所属栏目" required>
          <a-select v-model:value="editForm.columnId" placeholder="请选择所属栏目" :options="columnOptions" />
        </a-form-item>
        <a-form-item label="文章标题" required>
          <a-input v-model:value="editForm.title" placeholder="请输入文章标题（100 字内）" :maxlength="100" />
        </a-form-item>
        <a-form-item label="文章编码">
          <a-input
            v-model:value="editForm.articleCode"
            placeholder="唯一业务编码，App 按编码获取（如 user_agreement）"
            :maxlength="50"
          />
        </a-form-item>
        <a-form-item label="作者">
          <a-input v-model:value="editForm.author" placeholder="请输入作者（选填）" :maxlength="50" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="editForm.sortNo" :min="0" :precision="0" style="width: 140px" />
        </a-form-item>
        <a-form-item label="状态">
          <a-radio-group v-model:value="editForm.status">
            <a-radio :value="1">发布</a-radio>
            <a-radio :value="0">下架</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="摘要">
          <a-textarea v-model:value="editForm.summary" placeholder="摘要（选填，500 字内）" :rows="2" :maxlength="500" show-count />
        </a-form-item>
        <a-form-item label="文章内容" required>
          <JEditor v-model:value="editForm.content" height="360px" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="previewVisible" :title="previewTitle" width="760px" :footer="null">
      <div class="article-preview" v-html="previewContent"></div>
    </a-modal>
  </div>
</template>

<script lang="ts" name="article-admin" setup>
  import { h, ref } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import JEditor from '/@/components/Form/src/jeecg/components/JEditor.vue';
  import { list, queryById, addArticle, editArticle, deleteArticle, deleteBatch, columnList } from '/@/api/article/articleAdminApi';
  import { columns, searchFormSchema } from './data';

  const { createMessage } = useMessage();

  // 列表页面公共参数、方法
  const { tableContext } = useListPage({
    designScope: 'article-admin-template',
    tableProps: {
      title: '文章列表',
      api: list,
      columns: columns,
      actionColumn: {
        width: 180,
      },
      formConfig: {
        labelWidth: 90,
        schemas: searchFormSchema,
      },
    },
  });

  const [registerTable, { reload }, { rowSelection, selectedRowKeys }] = tableContext;

  const columnOptions = ref<{ label: string; value: string }[]>([]);
  const editVisible = ref(false);
  const editLoading = ref(false);
  const editForm = ref<any>(getDefaultForm());
  const previewVisible = ref(false);
  const previewTitle = ref('');
  const previewContent = ref('');

  function getDefaultForm() {
    return {
      id: '',
      columnId: undefined,
      articleCode: '',
      title: '',
      author: '',
      sortNo: 0,
      status: 1,
      summary: '',
      content: '',
    };
  }

  /** 加载栏目下拉 */
  async function loadColumns() {
    try {
      const res = await columnList();
      const records = res?.result || [];
      columnOptions.value = records.map((c) => ({ label: `${c.columnName}（${c.columnCode}）`, value: c.id }));
    } catch (e) {
      columnOptions.value = [];
    }
  }
  loadColumns();

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
        label: '预览',
        onClick: () => openPreview(record),
      },
      {
        label: '删除',
        popConfirm: {
          title: '是否确认删除',
          confirm: handleDelete.bind(null, record),
        },
      },
    ];
  }

  function openAdd() {
    editForm.value = getDefaultForm();
    loadColumns();
    editVisible.value = true;
  }

  async function openEdit(record) {
    loadColumns();
    const res = await queryById({ id: record.id });
    const detail = res?.result || record;
    editForm.value = {
      id: detail.id,
      columnId: detail.columnId,
      articleCode: detail.articleCode || '',
      title: detail.title,
      author: detail.author || '',
      sortNo: detail.sortNo ?? 0,
      status: detail.status ?? 1,
      summary: detail.summary || '',
      content: detail.content || '',
    };
    editVisible.value = true;
  }

  function openPreview(record) {
    previewTitle.value = record.title;
    // 列表不含正文，预览前拉取详情
    queryById({ id: record.id }).then((res) => {
      previewContent.value = res?.result?.content || '';
      previewVisible.value = true;
    });
  }

  async function handleSave() {
    const f = editForm.value;
    if (!f.columnId) {
      createMessage.warning('请选择所属栏目');
      return;
    }
    if (!f.title?.trim()) {
      createMessage.warning('请填写文章标题');
      return;
    }
    if (!f.content?.trim()) {
      createMessage.warning('请填写文章内容');
      return;
    }
    editLoading.value = true;
    try {
      const res = f.id ? await editArticle(f) : await addArticle(f);
      createMessage.success(res?.message || '保存成功');
      editVisible.value = false;
      reload();
    } finally {
      editLoading.value = false;
    }
  }

  async function handleDelete(record) {
    await deleteArticle({ id: record.id }, reload);
  }

  async function batchHandleDelete() {
    await deleteBatch({ ids: selectedRowKeys.value }, () => {
      selectedRowKeys.value = [];
      reload();
    });
  }
</script>

<style lang="less" scoped>
  .article-preview {
    max-height: 60vh;
    overflow: auto;
    line-height: 1.8;
    font-size: 14px;
    color: #333;

    h3 {
      font-size: 15px;
      font-weight: 600;
      margin: 16px 0 8px;
      color: #222;
    }

    p {
      margin: 0 0 10px;
      text-indent: 2em;
    }
  }
</style>
