<script setup lang="ts">
import { ref } from 'vue';
import { deleteAccount } from '@/api/account';
import { showSuccess, showError } from '@/utils';

interface Props {
  modelValue: boolean;
  accountId?: number | null;
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void;
  (e: 'success'): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const loading = ref(false);

const handleClose = () => {
  emit('update:modelValue', false);
};

const handleConfirm = async () => {
  if (!props.accountId) return;
  
  loading.value = true;
  try {
    const response = await deleteAccount({ id: props.accountId });
    if (response.code === 0 || response.code === 200) {
      showSuccess('账号删除成功');
      handleClose();
      emit('success');
    } else {
      throw new Error(response.msg || '删除失败');
    }
  } catch (error: any) {
    console.error('删除失败:', error);
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="删除账号确认"
    width="500px"
    @close="handleClose"
  >
    <div class="delete-confirm-content">
      <p class="confirm-text">确定要删除这个账号吗？</p>
      
      <el-alert
        type="error"
        :closable="false"
        show-icon
      >
        <template #title>
          <strong>重要提醒</strong>
        </template>
        <p>删除账号将会同时删除该账号下的所有相关数据，包括：</p>
        <ul>
          <li>聊天消息记录</li>
          <li>商品信息</li>
          <li>自动发货配置和记录</li>
          <li>自动回复配置和记录</li>
          <li>Cookie信息</li>
        </ul>
        <p><strong>此操作不可恢复，请谨慎操作！</strong></p>
      </el-alert>
    </div>
    
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="danger" :loading="loading" @click="handleConfirm">
        确定删除
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.delete-confirm-content {
  padding: 10px 0;
}

.confirm-text {
  margin: 0 0 20px 0;
  font-size: 16px;
  color: #303133;
}

.el-alert {
  margin-top: 20px;
}

.el-alert ul {
  margin: 10px 0;
  padding-left: 20px;
}

.el-alert li {
  margin: 5px 0;
  line-height: 1.6;
}

.el-alert p {
  margin: 10px 0;
  line-height: 1.6;
}
</style>
