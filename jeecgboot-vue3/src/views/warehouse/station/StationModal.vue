<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit" :width="800">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts" setup>
  import { ref, computed, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { formSchema } from './data';
  import { queryById, add, edit } from '/@/api/warehouse/stationApi';
  // 声明Emits
  const emit = defineEmits(['success', 'register']);
  const isUpdate = ref(true);
  const isDetail = ref(false);
  //表单配置
  const [registerForm, { resetFields, setFieldsValue, validate, setProps }] = useForm({
    schemas: formSchema,
    showActionButtonGroup: false,
  });
  //表单赋值
  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    //重置表单
    await resetFields();
    setModalProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;
    isDetail.value = !!data?.isDetail;
    //详情模式：表单只读，隐藏确定按钮
    setProps({ disabled: isDetail.value });
    setModalProps({ showOkBtn: !isDetail.value });
    if (unref(isUpdate) || unref(isDetail)) {
      //获取详情（district 为区县区域编码，JAreaSelect 组件自动反推省市回显）
      const record = await queryById({ id: data.record.id });
      //表单赋值
      await setFieldsValue({
        ...record,
      });
    }
  });
  //设置标题
  const getTitle = computed(() => (unref(isDetail) ? '站点详情' : unref(isUpdate) ? '编辑站点' : '新增站点'));
  //表单提交事件
  async function handleSubmit() {
    try {
      const values = await validate();
      //由区县编码反推省市编码入库（如 410505 -> 省 410000 / 市 410500）
      if (values.district) {
        values.province = values.district.substring(0, 2) + '0000';
        values.city = values.district.substring(0, 4) + '00';
      }
      setModalProps({ confirmLoading: true });
      //提交表单
      await (unref(isUpdate) ? edit(values) : add(values));
      //关闭弹窗
      closeModal();
      //刷新列表
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
<style lang="less" scoped>
  :deep(.ant-input-number) {
    width: 100%;
  }
</style>
