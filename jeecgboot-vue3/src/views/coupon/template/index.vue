<template>
  <div>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-button type="primary" pre-icon="ant-design:plus-outlined" @click="openAddModal">新增优惠券</a-button>
        <a-dropdown v-if="selectedRowKeys.length > 0" style="margin-left: 8px">
          <template #overlay>
            <a-menu>
              <a-menu-item key="1" @click="batchPublish">
                <Icon icon="ant-design:send-outlined"></Icon>
                批量发布
              </a-menu-item>
              <a-menu-item key="2" @click="batchOffline">
                <Icon icon="ant-design:stop-outlined"></Icon>
                批量下架
              </a-menu-item>
              <a-menu-item key="3" @click="batchDelete">
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

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="formVisible"
      :title="isEdit ? '编辑优惠券' : '新增优惠券'"
      width="640px"
      :confirm-loading="formLoading"
      @ok="handleFormSubmit"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" style="margin-top: 12px">
        <a-form-item label="券编码" required>
          <a-input v-model:value="formState.code" :disabled="isEdit" placeholder="业务固定编码，唯一，如 RUINS_PICKUP" allow-clear />
        </a-form-item>
        <a-form-item label="券名称" required>
          <a-input v-model:value="formState.couponName" placeholder="请输入券名称" allow-clear />
        </a-form-item>
        <a-form-item label="券类型" required>
          <a-select v-model:value="formState.couponType" :options="typeOptions" placeholder="请选择券类型" />
        </a-form-item>
        <a-form-item v-if="formState.couponType === 1 || formState.couponType === 2" :label="formState.couponType === 1 ? '优惠金额' : '折扣率'" required>
          <a-input-number v-model:value="formState.discountValue" :min="0.01" :max="9.99" :precision="2" style="width: 100%" />
          <div v-if="formState.couponType === 2" class="form-tip">折扣值0~10之间：填8.5表示8.5折，填0.95表示九五折（按原价95%结算）</div>
        </a-form-item>
        <a-form-item v-if="formState.couponType === 1" label="使用门槛">
          <a-input-number v-model:value="formState.minAmount" :min="0" :precision="2" style="width: 100%" placeholder="满多少可用，0为无门槛" />
        </a-form-item>
        <a-form-item label="有效期" required>
          <a-date-picker
            v-model:value="formState.validStartTime"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            placeholder="开始时间"
            style="width: 48%"
          />
          <span style="margin: 0 2%">~</span>
          <a-date-picker
            v-model:value="formState.validEndTime"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            placeholder="结束时间"
            style="width: 48%"
          />
        </a-form-item>
        <a-form-item label="总库存" required>
          <a-input-number v-model:value="formState.totalCount" :min="0" :precision="0" style="width: 100%" :disabled="isEdit" />
          <div v-if="isEdit" class="form-tip">编辑不可修改库存，请使用列表中的「库存调整」</div>
        </a-form-item>
        <a-form-item label="每人限领">
          <a-input-number v-model:value="formState.perLimit" :min="0" :precision="0" style="width: 100%" />
          <div class="form-tip">0为不限，超过限制不可再次领取</div>
        </a-form-item>
        <a-form-item label="每日限领">
          <a-input-number v-model:value="formState.dailyLimit" :min="0" :precision="0" style="width: 100%" />
          <div class="form-tip">0为不限，App每日领取按此数量限制（拾取券每日领取至少限1张）</div>
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="formState.sort" :precision="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="券描述">
          <a-textarea v-model:value="formState.description" :rows="2" placeholder="App端展示的券说明" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="formState.remark" :rows="2" placeholder="后台备注，App端不可见" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 库存调整弹窗 -->
    <a-modal v-model:open="stockVisible" title="库存调整" :confirm-loading="stockLoading" @ok="handleStockSubmit">
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" style="margin-top: 12px">
        <a-form-item label="券名称">
          <span>{{ stockCoupon?.couponName }}</span>
        </a-form-item>
        <a-form-item label="当前库存">
          <span>总量 {{ stockCoupon?.totalCount }}，剩余 {{ stockCoupon?.remainCount }}，已使用 {{ stockCoupon?.usedCount }}</span>
        </a-form-item>
        <a-form-item label="调整数量" required>
          <a-input-number v-model:value="stockDelta" :precision="0" style="width: 100%" placeholder="正数增加库存，负数减少库存" />
          <div class="form-tip">调整后剩余库存不能为负数；总量与剩余同步调整</div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" name="coupon-template" setup>
  import { reactive, ref } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { list, add, edit, deleteCoupon, deleteBatch, publishBatch, offlineBatch, adjustStock } from '/@/api/coupon/couponAdminApi';
  import { columns, searchFormSchema, COUPON_TYPE_OPTIONS } from './data';

  const { createMessage, createConfirm } = useMessage();

  // 列表页面公共参数、方法
  const { tableContext } = useListPage({
    designScope: 'coupon-template',
    tableProps: {
      title: '优惠券列表',
      api: list,
      columns: columns,
      actionColumn: {
        width: 200,
      },
      formConfig: {
        labelWidth: 80,
        schemas: searchFormSchema,
      },
    },
  });

  const [registerTable, { reload }, { rowSelection, selectedRowKeys }] = tableContext;

  const typeOptions = COUPON_TYPE_OPTIONS.map(({ label, value }) => ({ label, value }));

  const formVisible = ref(false);
  const formLoading = ref(false);
  const isEdit = ref(false);
  const formState = reactive(getDefaultFormState());

  const stockVisible = ref(false);
  const stockLoading = ref(false);
  const stockCoupon = ref(null);
  const stockDelta = ref(0);

  function getDefaultFormState() {
    return {
      id: '',
      code: '',
      couponName: '',
      couponType: 1,
      discountValue: null,
      minAmount: 0,
      validStartTime: '',
      validEndTime: '',
      totalCount: 100,
      perLimit: 0,
      dailyLimit: 0,
      sort: 0,
      description: '',
      remark: '',
    };
  }

  /**
   * 操作列定义
   */
  function getActions(record) {
    const actions = [
      {
        label: '编辑',
        onClick: () => openEditModal(record),
      },
    ];
    if (record.status == 1) {
      actions.push({
        label: '下架',
        popConfirm: {
          title: '下架后App端不可再领取该券，是否继续？',
          confirm: handleOffline.bind(null, record),
        },
      });
    } else {
      actions.push({
        label: '发布',
        popConfirm: {
          title: '发布后App端可领取该券，是否继续？',
          confirm: handlePublish.bind(null, record),
        },
      });
    }
    actions.push({
      label: '库存调整',
      onClick: () => openStockModal(record),
    });
    actions.push({
      label: '删除',
      popConfirm: {
        title: '已有用户持券记录的券将被拒绝删除，是否继续？',
        confirm: handleDelete.bind(null, record),
      },
    });
    return actions;
  }

  /**
   * 打开新增弹窗
   */
  function openAddModal() {
    isEdit.value = false;
    Object.assign(formState, getDefaultFormState());
    formVisible.value = true;
  }

  /**
   * 打开编辑弹窗
   */
  function openEditModal(record) {
    isEdit.value = true;
    Object.assign(formState, getDefaultFormState(), {
      id: record.id,
      code: record.code,
      couponName: record.couponName,
      couponType: record.couponType,
      discountValue: record.discountValue,
      minAmount: record.minAmount,
      validStartTime: record.validStartTime,
      validEndTime: record.validEndTime,
      totalCount: record.totalCount,
      perLimit: record.perLimit ?? 0,
      dailyLimit: record.dailyLimit ?? 0,
      sort: record.sort ?? 0,
      description: record.description,
      remark: record.remark,
    });
    formVisible.value = true;
  }

  /**
   * 新增/编辑提交
   */
  async function handleFormSubmit() {
    if (!formState.code || !formState.code.trim()) {
      createMessage.warning('请填写券编码');
      return;
    }
    if (!formState.couponName || !formState.couponName.trim()) {
      createMessage.warning('请填写券名称');
      return;
    }
    if (!formState.couponType) {
      createMessage.warning('请选择券类型');
      return;
    }
    if ((formState.couponType === 1 || formState.couponType === 2) && !(formState.discountValue > 0)) {
      createMessage.warning('满减/折扣券必须填写大于0的面额');
      return;
    }
    if (formState.couponType === 2 && !(formState.discountValue > 0 && formState.discountValue < 10)) {
      createMessage.warning('折扣值应在0~10之间（填8.5表示8.5折，填0.95表示九五折）');
      return;
    }
    if (!formState.validStartTime || !formState.validEndTime) {
      createMessage.warning('请选择有效期');
      return;
    }
    if (formState.validEndTime <= formState.validStartTime) {
      createMessage.warning('有效期结束时间必须晚于开始时间');
      return;
    }
    if (!isEdit.value && formState.totalCount == null) {
      createMessage.warning('请填写总库存');
      return;
    }
    formLoading.value = true;
    try {
      const res = isEdit.value ? await edit(formState) : await add(formState);
      createMessage.success(res?.message || (isEdit.value ? '更新成功' : '添加成功'));
      formVisible.value = false;
      reload();
    } finally {
      formLoading.value = false;
    }
  }

  /**
   * 单条发布
   */
  async function handlePublish(record) {
    const res = await publishBatch({ ids: [record.id] });
    createMessage.success(res?.message || '发布成功');
    reload();
  }

  /**
   * 单条下架
   */
  async function handleOffline(record) {
    const res = await offlineBatch({ ids: [record.id] });
    createMessage.success(res?.message || '下架成功');
    reload();
  }

  /**
   * 打开库存调整弹窗
   */
  function openStockModal(record) {
    stockCoupon.value = record;
    stockDelta.value = null;
    stockVisible.value = true;
  }

  /**
   * 库存调整提交
   */
  async function handleStockSubmit() {
    if (!stockDelta.value) {
      createMessage.warning('请填写调整数量（不能为0）');
      return;
    }
    stockLoading.value = true;
    try {
      const res = await adjustStock({ id: stockCoupon.value.id, delta: stockDelta.value });
      createMessage.success(res?.message || '库存调整成功');
      stockVisible.value = false;
      reload();
    } finally {
      stockLoading.value = false;
    }
  }

  /**
   * 单条删除
   */
  async function handleDelete(record) {
    await deleteCoupon({ id: record.id }, reload);
  }

  /**
   * 批量发布
   */
  function batchPublish() {
    createConfirm({
      iconType: 'warning',
      title: '确认发布',
      content: '仅未发布/已下架状态的券会被发布，是否继续？',
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const res = await publishBatch({ ids: selectedRowKeys.value });
        createMessage.success(res?.message || '批量发布成功');
        selectedRowKeys.value = [];
        reload();
      },
    });
  }

  /**
   * 批量下架
   */
  function batchOffline() {
    createConfirm({
      iconType: 'warning',
      title: '确认下架',
      content: '仅已发布状态的券会被下架，下架后App不可领取，是否继续？',
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const res = await offlineBatch({ ids: selectedRowKeys.value });
        createMessage.success(res?.message || '批量下架成功');
        selectedRowKeys.value = [];
        reload();
      },
    });
  }

  /**
   * 批量删除
   */
  async function batchDelete() {
    await deleteBatch({ ids: selectedRowKeys.value }, () => {
      selectedRowKeys.value = [];
      reload();
    });
  }
</script>

<style lang="less" scoped>
  .form-tip {
    color: #999;
    font-size: 12px;
    line-height: 1.6;
  }
</style>
