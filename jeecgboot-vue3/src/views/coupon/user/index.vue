<template>
  <div>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-button type="primary" pre-icon="ant-design:gift-outlined" @click="openGrantModal">发放券</a-button>
        <a-button
          v-if="selectedRowKeys.length > 0"
          danger
          style="margin-left: 8px"
          pre-icon="ant-design:rest-outlined"
          @click="batchInvalidate"
        >
          批量作废
        </a-button>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" />
      </template>
    </BasicTable>

    <!-- 发放券弹窗 -->
    <a-modal v-model:open="grantVisible" title="发放优惠券" :confirm-loading="grantLoading" @ok="handleGrant">
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" style="margin-top: 12px">
        <a-form-item label="用户" required>
          <a-input v-model:value="grantState.userKey" placeholder="手机号 / 用户名 / 用户Id" allow-clear />
        </a-form-item>
        <a-form-item label="优惠券" required>
          <a-select
            v-model:value="grantState.couponId"
            :options="couponOptions"
            placeholder="仅可选择已发布的优惠券"
            show-search
            option-filter-prop="label"
          />
        </a-form-item>
        <a-form-item label="发放张数" required>
          <a-input-number v-model:value="grantState.count" :min="1" :max="999" :precision="0" style="width: 100%" />
        </a-form-item>
      </a-form>
      <p class="grant-tip">
        发放方式为「系统发放」，发放成功即扣减券库存；库存不足时发放失败。
      </p>
    </a-modal>
  </div>
</template>

<script lang="ts" name="coupon-user" setup>
  import { onMounted, reactive, ref } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { userCouponList, list, grantCoupon, invalidateBatch } from '/@/api/coupon/couponAdminApi';
  import { columns, searchFormSchema } from './data';

  const { createMessage, createConfirm } = useMessage();

  // 列表页面公共参数、方法
  const { tableContext } = useListPage({
    designScope: 'coupon-user',
    tableProps: {
      title: '用户持券列表',
      api: userCouponList,
      columns: columns,
      actionColumn: {
        width: 100,
      },
      formConfig: {
        labelWidth: 80,
        schemas: searchFormSchema,
      },
    },
  });

  const [registerTable, { reload }, { rowSelection, selectedRowKeys }] = tableContext;

  const grantVisible = ref(false);
  const grantLoading = ref(false);
  const grantState = reactive({ userKey: '', couponId: undefined as string, count: 1 });
  const couponOptions = ref<{ label: string; value: string }[]>([]);

  /**
   * 加载可发放的券（仅已发布）
   */
  async function loadGrantableCoupons() {
    const res = await list({ pageNo: 1, pageSize: 100, status: 1 });
    const records = res?.records || res?.result?.records || [];
    couponOptions.value = records.map((c) => ({
      label: `${c.couponName}（剩余${c.remainCount}）`,
      value: String(c.id),
    }));
  }

  onMounted(() => {
    loadGrantableCoupons();
  });

  /**
   * 操作列定义
   */
  function getActions(record) {
    const actions = [];
    if (record.status == 0) {
      actions.push({
        label: '作废',
        popConfirm: {
          title: '作废后该券不可使用且回补库存，是否继续？',
          confirm: handleInvalidate.bind(null, record),
        },
      });
    }
    return actions;
  }

  /**
   * 打开发放弹窗
   */
  async function openGrantModal() {
    grantState.userKey = '';
    grantState.couponId = undefined;
    grantState.count = 1;
    grantVisible.value = true;
    await loadGrantableCoupons();
  }

  /**
   * 发放提交
   */
  async function handleGrant() {
    if (!grantState.userKey.trim()) {
      createMessage.warning('请填写要发放的用户（手机号/用户名/用户Id）');
      return;
    }
    if (!grantState.couponId) {
      createMessage.warning('请选择要发放的优惠券');
      return;
    }
    if (!grantState.count || grantState.count < 1) {
      createMessage.warning('发放张数必须大于0');
      return;
    }
    grantLoading.value = true;
    try {
      const res = await grantCoupon({ userKey: grantState.userKey.trim(), couponId: grantState.couponId, count: grantState.count });
      createMessage.success(res?.message || '发放成功');
      grantVisible.value = false;
      reload();
    } finally {
      grantLoading.value = false;
    }
  }

  /**
   * 单条作废
   */
  async function handleInvalidate(record) {
    const res = await invalidateBatch({ ids: [record.id] });
    createMessage.success(res?.message || '作废成功');
    reload();
  }

  /**
   * 批量作废（仅未使用状态可作废）
   */
  function batchInvalidate() {
    createConfirm({
      iconType: 'warning',
      title: '确认作废',
      content: '仅未使用状态的券会被作废，作废后不可使用且回补库存，是否继续？',
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const res = await invalidateBatch({ ids: selectedRowKeys.value });
        createMessage.success(res?.message || '批量作废成功');
        selectedRowKeys.value = [];
        reload();
      },
    });
  }
</script>

<style lang="less" scoped>
  .grant-tip {
    margin: 4px 0 0 8px;
    color: #999;
    font-size: 12px;
    line-height: 1.6;
  }
</style>
