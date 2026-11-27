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

    <!-- 新增/编辑抽屉（复用栏目下拉与富文本编辑） -->
    <ArticleDrawer @register="registerDrawer" @success="reload" />

    <a-modal v-model:open="previewVisible" :title="previewTitle" width="760px" :footer="null">
      <div class="article-preview" v-html="previewContent"></div>
    </a-modal>
  </div>
</template>

<script lang="ts" name="article-admin" setup>
  import { ref } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { list, queryById, deleteArticle, deleteBatch } from '/@/api/article/articleAdminApi';
  import { columns, searchFormSchema } from './data';
  import ArticleDrawer from './ArticleDrawer.vue';

  // 列表页面公共参数、方法
  const { tableContext } = useListPage({
    designScope: 'article-admin-template',
    tableProps: {
      title: '文章列表',
      api: list,
      columns: columns,
      actionColumn: {
        width: 180,
        fixed: 'right',
      },
      formConfig: {
        labelWidth: 90,
        schemas: searchFormSchema,
      },
    },
  });

  const [registerTable, { reload }, { rowSelection, selectedRowKeys }] = tableContext;

  // 新增/编辑抽屉
  const [registerDrawer, { openDrawer }] = useDrawer();

  const previewVisible = ref(false);
  const previewTitle = ref('');
  const previewContent = ref('');

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
    openDrawer(true, { isUpdate: false });
  }

  function openEdit(record) {
    // 详情（含富文本内容）由抽屉内部拉取
    openDrawer(true, { isUpdate: true, record });
  }

  function openPreview(record) {
    previewTitle.value = record.title;
    // 列表不含正文，预览前拉取详情；返回值即详情对象
    queryById({ id: record.id }).then((res) => {
      previewContent.value = res?.content || '';
      previewVisible.value = true;
    });
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
