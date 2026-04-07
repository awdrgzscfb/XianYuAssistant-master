<script setup lang="ts">
import { getAccountStatusText, formatTime } from '@/utils'
import type { Account } from '@/types'

interface Props {
  accounts: Account[]
  loading?: boolean
}

interface Emits {
  (e: 'edit', account: Account): void
  (e: 'delete', id: number): void
}

defineProps<Props>()
const emit = defineEmits<Emits>()
</script>

<template>
  <el-table :data="accounts" v-loading="loading" stripe>
    <el-table-column prop="id" label="ID" width="80" />
    <el-table-column prop="unb" label="UNB" min-width="150" />
    <el-table-column prop="accountNote" label="账号备注" min-width="150">
      <template #default="{ row }">
        {{ row.accountNote || '未命名账号' }}
      </template>
    </el-table-column>
    <el-table-column prop="status" label="状态" width="100">
      <template #default="{ row }">
        <el-tag :type="getAccountStatusText(row.status).type">
          {{ getAccountStatusText(row.status).text }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="createdTime" label="创建时间" width="180">
      <template #default="{ row }">
        {{ formatTime(row.createdTime) }}
      </template>
    </el-table-column>
    <el-table-column prop="updatedTime" label="更新时间" width="180">
      <template #default="{ row }">
        {{ formatTime(row.updatedTime) }}
      </template>
    </el-table-column>
    <el-table-column label="操作" width="180" fixed="right">
      <template #default="{ row }">
        <el-button size="small" @click="emit('edit', row)">编辑</el-button>
        <el-button size="small" type="danger" @click="emit('delete', row.id)">删除</el-button>
      </template>
    </el-table-column>
    
    <template #empty>
      <el-empty description="暂无账号数据" />
    </template>
  </el-table>
</template>
