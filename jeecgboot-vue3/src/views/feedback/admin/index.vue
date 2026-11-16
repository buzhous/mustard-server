<template>
  <div>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-dropdown v-if="selectedRowKeys.length > 0">
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
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'content'">
          <a-tooltip :title="record.content">
            <span>{{ record.content }}</span>
          </a-tooltip>
        </template>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" />
      </template>
    </BasicTable>

    <a-modal
      v-model:open="replyVisible"
      title="回复反馈"
      :confirm-loading="replyLoading"
      ok-text="提交回复"
      @ok="handleReply"
    >
      <p class="reply-quote">
        <span class="reply-label">用户反馈：</span>
        <span>{{ replyRecord?.content }}</span>
      </p>
      <a-textarea
        v-model:value="replyContent"
        placeholder="填写回复内容（写入后反馈标记为已处理）"
        :rows="4"
        :maxlength="500"
        show-count
      />
    </a-modal>
  </div>
</template>

<script lang="ts" name="feedback-admin" setup>
  import { ref } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { list, replyFeedback, deleteFeedback, deleteBatch } from '/@/api/feedback/feedbackAdminApi';
  import { columns, searchFormSchema } from './data';

  const { createMessage } = useMessage();

  // 列表页面公共参数、方法
  const { tableContext } = useListPage({
    designScope: 'feedback-admin-template',
    tableProps: {
      title: '用户反馈列表',
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

  const [registerTable, { reload }, { rowSelection, selectedRowKeys }] = tableContext;

  const replyVisible = ref(false);
  const replyLoading = ref(false);
  const replyRecord = ref<Recordable>(null);
  const replyContent = ref('');

  /**
   * 操作列定义
   * @param record
   */
  function getActions(record) {
    return [
      {
        label: '回复',
        onClick: () => openReply(record),
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

  /**
   * 打开回复弹窗
   * @param record
   */
  function openReply(record) {
    replyRecord.value = record;
    replyContent.value = record.reply || '';
    replyVisible.value = true;
  }

  /**
   * 提交回复（后端写入回复并标记已处理）
   */
  async function handleReply() {
    if (!replyContent.value.trim()) {
      createMessage.warning('请填写回复内容');
      return;
    }
    replyLoading.value = true;
    try {
      const res = await replyFeedback({ id: replyRecord.value.id, reply: replyContent.value.trim() });
      createMessage.success(res?.message || '回复成功');
      replyVisible.value = false;
      reload();
    } finally {
      replyLoading.value = false;
    }
  }

  /**
   * 单条删除
   */
  async function handleDelete(record) {
    await deleteFeedback({ id: record.id }, reload);
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
</script>

<style lang="less" scoped>
  .reply-quote {
    margin-bottom: 12px;
    padding: 8px 12px;
    background: #f5f5f7;
    border-radius: 4px;
    font-size: 12px;
    color: #555;
    line-height: 1.6;

    .reply-label {
      color: #999;
    }
  }
</style>
