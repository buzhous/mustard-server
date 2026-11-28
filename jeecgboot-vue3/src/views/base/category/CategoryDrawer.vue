<template>
  <BasicDrawer
    v-bind="$attrs"
    @register="registerDrawer"
    :title="getTitle"
    width="60%"
    showFooter
    destroyOnClose
    @ok="handleSubmit"
  >
    <BasicForm @register="registerForm" />
    <a-alert
      class="drawer-tip"
      type="info"
      show-icon
      message="保存成功后主题管理「关联分类」下拉与 App 系统分类列表即时生效；名称全局唯一，停用不物理删除（已实例化的用户分类仍指向该行）。"
    />
  </BasicDrawer>
</template>
<script lang="ts" setup>
  import { ref, computed, unref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { formSchema } from './data';
  import { add, edit } from '/@/api/base/categoryApi';
  // 声明Emits
  const emit = defineEmits(['success', 'register']);
  const isUpdate = ref(true);

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    schemas: formSchema,
    labelWidth: 110,
    showActionButtonGroup: false,
    baseColProps: { span: 12 },
  });

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
    await resetFields();
    setDrawerProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;
    if (unref(isUpdate)) {
      // 编辑回填（列表行数据即详情）
      await setFieldsValue({ ...data.record });
    }
  });

  const getTitle = computed(() => (unref(isUpdate) ? '编辑系统分类' : '新增系统分类'));

  async function handleSubmit() {
    try {
      const values = await validate();
      setDrawerProps({ confirmLoading: true });
      // 名称唯一校验由后端完成（uk_system_category_name + service 预检）
      await (unref(isUpdate) ? edit(values) : add(values));
      closeDrawer();
      emit('success');
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }
</script>
<style lang="less" scoped>
  .drawer-tip {
    margin-bottom: 16px;
  }
</style>
