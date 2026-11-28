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
      message="保存成功后：字典缓存立即刷新、版本 +1 并续期 365 天；App 下次登录/初始化按本端（platform = ios / android）比对版本后自动同步，仅加载已勾选本端的字段。已录入字段不可删除，仅支持停用。"
    />
  </BasicDrawer>
</template>
<script lang="ts" setup>
  import { ref, computed, unref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { formSchema, REQUIRED_ELEMENTS } from './data';
  import { queryById, add, edit } from '/@/api/base/fieldElementApi';
  // 声明Emits
  const emit = defineEmits(['success', 'register']);
  const isUpdate = ref(true);

  const [registerForm, { resetFields, setFieldsValue, updateSchema, validate }] = useForm({
    schemas: formSchema,
    labelWidth: 110,
    showActionButtonGroup: false,
    baseColProps: { span: 12 },
  });

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
    await resetFields();
    setDrawerProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;
    // 组件编号：编辑时锁定不可改；新增时留空由后端自动顺延分配（35 起）
    await updateSchema([
      {
        field: 'element',
        componentProps: { disabled: unref(isUpdate) },
        helpMessage: unref(isUpdate)
          ? '编辑时组件编号锁定不可改'
          : '全局唯一；内置 1-34，扩展新增可留空由后端自动顺延分配（35 起）',
      },
    ]);
    if (unref(isUpdate)) {
      // 获取详情
      const record = await queryById({ id: data.record.id });
      await setFieldsValue({ ...record, platformFlags: buildFlags(record) });
      // 必填三要素（1 图标 / 2 名称 / 3 类别）锁定必填属性
      const lockedRequired = REQUIRED_ELEMENTS.includes(record.element);
      await updateSchema([
        {
          field: 'isRequired',
          componentProps: { disabled: lockedRequired },
          helpMessage: lockedRequired ? '必填三要素锁定必填' : '',
        },
      ]);
      if (lockedRequired) {
        await setFieldsValue({ isRequired: 1 });
      }
    } else {
      await updateSchema([
        {
          field: 'isRequired',
          componentProps: { disabled: false },
          helpMessage: '',
        },
      ]);
    }
  });

  const getTitle = computed(() => (unref(isUpdate) ? '编辑字段' : '新增字段'));

  /** 后端 androidFlag/iosFlag → 前端勾选数组 */
  function buildFlags(record) {
    const flags: string[] = [];
    if (record.androidFlag == 1) flags.push('android');
    if (record.iosFlag == 1) flags.push('ios');
    return flags;
  }

  async function handleSubmit() {
    try {
      const values = await validate();
      const { platformFlags, ...rest } = values;
      const params = {
        ...rest,
        // 新增时组件编号留空 → 后端自动顺延分配；编辑时后端以库内编号为准
        androidFlag: platformFlags?.includes('android') ? 1 : 0,
        iosFlag: platformFlags?.includes('ios') ? 1 : 0,
      };
      setDrawerProps({ confirmLoading: true });
      await (unref(isUpdate) ? edit(params) : add(params));
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
