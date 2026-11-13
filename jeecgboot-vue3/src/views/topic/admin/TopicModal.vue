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
    <!-- 基本信息 -->
    <div class="section-title">基本信息</div>
    <BasicForm @register="registerForm" />
    <!-- 物品字段配置 -->
    <div class="section-title" style="margin-top: 18px">物品字段配置</div>
    <FieldConfigEditor ref="fieldEditorRef" :key="editorSessionKey" v-model:value="fieldsValue" />
  </BasicDrawer>
</template>
<script lang="ts" setup>
  import { ref, computed, unref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { formSchema } from './data';
  import FieldConfigEditor from './FieldConfigEditor.vue';
  import { queryById, add, edit } from '/@/api/topic/topicAdminApi';
  import { useMessage } from '/@/hooks/web/useMessage';
  // 声明Emits
  const emit = defineEmits(['success', 'register']);
  const { createErrorNotification } = useMessage();
  const isUpdate = ref(true);
  const fieldsValue = ref('');
  const fieldEditorRef = ref();
  // 每次打开抽屉递增，强制重建字段配置编辑器，避免上一会话残留状态叠加
  const editorSessionKey = ref(0);

  //表单配置（基本信息一行两列，宽字段单独占满一行）
  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    schemas: formSchema,
    labelWidth: 100,
    showActionButtonGroup: false,
    baseColProps: { span: 12 },
  });
  //抽屉赋值
  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
    // 先清空上次会话状态并重建编辑器，避免字段配置数据残留/重复
    fieldsValue.value = '';
    editorSessionKey.value++;
    //重置表单
    await resetFields();
    setDrawerProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;
    if (unref(isUpdate)) {
      //获取详情
      const record = await queryById({ id: data.record.id });
      fieldsValue.value = record.fields || '';
      //表单赋值
      await setFieldsValue({
        ...record,
      });
    } else {
      fieldsValue.value = '';
    }
  });
  //设置标题
  const getTitle = computed(() => (unref(isUpdate) ? '编辑主题' : '新增主题'));
  //提交事件
  async function handleSubmit() {
    const fieldError = fieldEditorRef.value?.validate?.();
    if (fieldError) {
      createErrorNotification({ message: fieldError });
      return;
    }
    try {
      const values = await validate();
      values.fields = fieldsValue.value;
      setDrawerProps({ confirmLoading: true });
      //提交表单（主题类型由后端根据 fields 自动推导）
      await (unref(isUpdate) ? edit(values) : add(values));
      //关闭抽屉
      closeDrawer();
      //刷新列表
      emit('success');
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }
</script>
<style lang="less" scoped>
  .section-title {
    position: relative;
    padding-left: 10px;
    margin-bottom: 14px;
    font-weight: 600;
    font-size: 14px;
    color: rgba(0, 0, 0, 0.85);

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 3px;
      bottom: 3px;
      width: 3px;
      background: @primary-color;
      border-radius: 2px;
    }
  }
</style>
