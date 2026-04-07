<script setup lang="ts">
import { ref, watch } from 'vue'
import { updateAccount } from '@/api/account'
import { showSuccess, showError } from '@/utils'
import type { Account } from '@/types'

interface Props {
  modelValue: boolean
  account?: Account | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const formData = ref({
  accountId: 0,
  accountNote: ''
})

watch(() => props.account, (newAccount) => {
  if (newAccount) {
    formData.value = {
      accountId: newAccount.id,
      accountNote: newAccount.accountNote || ''
    }
  } else {
    formData.value = {
      accountId: 0,
      accountNote: ''
    }
  }
}, { immediate: true })

const handleClose = () => {
  emit('update:modelValue', false)
}

const handleSubmit = async () => {
  if (!formData.value.accountNote.trim()) {
    showError('请输入账号备注')
    return
  }

  try {
    const response = await updateAccount(formData.value)
    if (response.code === 0 || response.code === 200) {
      showSuccess('保存成功')
      handleClose()
      emit('success')
    } else {
      throw new Error(response.msg || '保存失败')
    }
  } catch (error: any) {
    console.error('保存失败:', error)
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="account ? '编辑账号' : '添加账号'"
    width="500px"
    @close="handleClose"
  >
    <el-form :model="formData" label-width="100px">
      <el-form-item label="账号备注">
        <el-input v-model="formData.accountNote" placeholder="请输入账号备注" />
      </el-form-item>
    </el-form>
    
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>
