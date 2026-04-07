<script setup lang="ts">
import { ref, watch } from 'vue'
import { updateCookie } from '@/api/websocket'
import { showSuccess, showError } from '@/utils'

interface Props {
  modelValue: boolean
  accountId: number
  currentCookie: string
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const cookieText = ref('')
const loading = ref(false)

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    cookieText.value = props.currentCookie || ''
  }
})

const handleSubmit = async () => {
  if (!cookieText.value.trim()) {
    showError('Cookie不能为空')
    return
  }

  loading.value = true
  try {
    const response = await updateCookie({
      xianyuAccountId: props.accountId,
      cookieText: cookieText.value.trim()
    })

    // request拦截器会自动处理错误，这里只处理成功的情况
    if (response.code === 200) {
      showSuccess('Cookie更新成功')
      handleClose()
      emit('success')
    }
  } catch (error: any) {
    // request拦截器已经显示了错误消息，这里不需要再显示
    // 只记录日志用于调试
    console.error('Cookie更新失败:', error)
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  emit('update:modelValue', false)
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="手动更新Cookie"
    width="600px"
    @close="handleClose"
  >
    <el-form label-width="80px">
      <el-form-item label="Cookie值">
        <el-input
          v-model="cookieText"
          type="textarea"
          :rows="8"
          placeholder="请输入完整的Cookie字符串"
          class="cookie-input"
        />
      </el-form-item>
      <el-alert
        title="提示"
        type="info"
        :closable="false"
        show-icon
      >
        <p style="margin-bottom: 8px;">请从浏览器中复制完整的Cookie字符串</p>
        <p style="margin-bottom: 8px; font-size: 12px;">
          <span style="color: #e6a23c; font-weight: 600;">重要字段：</span>
          <span style="color: #f56c6c; font-weight: 500;">unb</span>、
          <span style="color: #f56c6c; font-weight: 500;">_m_h5_tk</span>、
          <span style="color: #f56c6c; font-weight: 500;">cookie2</span>、
          <span style="color: #f56c6c; font-weight: 500;">t</span>
        </p>
        <p style="margin-bottom: 4px; font-weight: 500;">格式示例：</p>
        <p style="font-size: 11px; line-height: 1.8; word-break: break-all; font-family: 'Courier New', Consolas, monospace;">
          <span style="color: #f56c6c; font-weight: 600;">unb</span>=2218021801256; 
          cookies=sgcookie=E100JgD87TWZ...; 
          <span style="color: #f56c6c; font-weight: 600;">t</span>=97df36d73d5e5bfb...; 
          tracknick=xy246940070033; 
          csg=f7aeab6d; 
          _m_h5_tk_enc=51ce7936ea...; 
          XSRF-TOKEN=bb76b331-48fb-496b...; 
          _samesite_flag_=true; 
          mtop_partitioned_detect=1; 
          <span style="color: #f56c6c; font-weight: 600;">_m_h5_tk</span>=5f73f84e8caa...; 
          _tb_token_=e3f1fd5ee5a34; 
          <span style="color: #f56c6c; font-weight: 600;">cookie2</span>=153aeae482f715e0...
        </p>
      </el-alert>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        确定更新
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.cookie-input :deep(.el-textarea__inner) {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 12px;
  line-height: 1.5;
}

.el-alert {
  margin-top: 10px;
}

.el-alert p {
  margin: 5px 0;
  font-size: 13px;
}
</style>
