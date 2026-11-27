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
    <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 19 }">
      <a-form-item label="所属栏目" required>
        <a-select v-model:value="form.columnId" placeholder="请选择所属栏目" :options="columnOptions" />
      </a-form-item>
      <a-form-item label="文章标题" required>
        <a-input v-model:value="form.title" placeholder="请输入文章标题（100 字内）" :maxlength="100" />
      </a-form-item>
      <a-form-item label="文章编码">
        <a-input
          v-model:value="form.articleCode"
          placeholder="唯一业务编码，App 按编码获取（如 user_agreement）"
          :maxlength="50"
        />
      </a-form-item>
      <a-form-item label="作者">
        <a-input v-model:value="form.author" placeholder="请输入作者（选填）" :maxlength="50" />
      </a-form-item>
      <a-form-item label="排序">
        <a-input-number v-model:value="form.sortNo" :min="0" :precision="0" style="width: 140px" />
      </a-form-item>
      <a-form-item label="状态">
        <a-radio-group v-model:value="form.status">
          <a-radio :value="1">发布</a-radio>
          <a-radio :value="0">下架</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="摘要">
        <a-textarea v-model:value="form.summary" placeholder="摘要（选填，500 字内）" :rows="2" :maxlength="500" show-count />
      </a-form-item>
      <a-form-item label="文章内容" required>
        <JEditor v-model:value="form.content" height="360px" />
      </a-form-item>
    </a-form>
  </BasicDrawer>
</template>

<script lang="ts" setup name="article-article-drawer">
  import { ref, computed, unref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import JEditor from '/@/components/Form/src/jeecg/components/JEditor.vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { queryById, addArticle, editArticle, columnList } from '/@/api/article/articleAdminApi';

  // 声明Emits
  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();
  const isUpdate = ref(true);
  const form = ref<any>(getDefaultForm());
  const columnOptions = ref<{ label: string; value: string }[]>([]);

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

  const getTitle = computed(() => (unref(isUpdate) ? '编辑文章' : '新增文章'));

  //抽屉赋值
  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
    isUpdate.value = !!data?.isUpdate;
    setDrawerProps({ confirmLoading: false });
    await loadColumns();
    if (unref(isUpdate)) {
      const res = await queryById({ id: data.record.id });
      // defHttp 默认解包 Result：返回值即文章详情对象；
      // 列表行不含富文本内容（content 为 null），必须以详情为准，否则编辑内容恒为空
      const detail = res || data.record;
      form.value = {
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
    } else {
      form.value = getDefaultForm();
    }
  });

  /** 加载栏目下拉 */
  async function loadColumns() {
    try {
      const res = await columnList();
      // defHttp 默认解包 Result：返回值即栏目数组（Result.ok(list)）
      const records = res || [];
      columnOptions.value = records.map((c) => ({ label: `${c.columnName}（${c.columnCode}）`, value: c.id }));
    } catch (e) {
      columnOptions.value = [];
    }
  }

  //提交事件
  async function handleSubmit() {
    const f = form.value;
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
    setDrawerProps({ confirmLoading: true });
    try {
      const res = f.id ? await editArticle(f) : await addArticle(f);
      createMessage.success(res?.message || '保存成功');
      closeDrawer();
      //刷新列表
      emit('success');
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }
</script>
