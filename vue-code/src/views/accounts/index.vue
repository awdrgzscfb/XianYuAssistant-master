<script setup lang="ts">
import { useAccountManager } from './useAccountManager'
import AccountTable from './components/AccountTable.vue'
import AddAccountDialog from './components/AddAccountDialog.vue'
import ManualAddDialog from './components/ManualAddDialog.vue'
import QRLoginDialog from './components/QRLoginDialog.vue'
import DeleteConfirmDialog from './components/DeleteConfirmDialog.vue'

const {
  loading,
  accounts,
  dialogs,
  currentAccount,
  deleteAccountId,
  loadAccounts,
  showAddDialog,
  showManualAddDialog,
  showQRLoginDialog,
  editAccount,
  deleteAccount
} = useAccountManager();

// 组件挂载时加载数据
loadAccounts();
</script>

<template>
  <div class="accounts-page">
    <div class="page-header">
      <h1 class="page-title">闲鱼账号</h1>
      <div class="header-actions">
        <el-button type="primary" @click="showQRLoginDialog">
          📱 扫码添加闲鱼账号
        </el-button>
        <el-button @click="showManualAddDialog">
          + 手动添加
        </el-button>
      </div>
    </div>

    <el-card class="account-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">闲鱼账号列表</span>
          <el-button 
            type="primary" 
            link 
            @click="loadAccounts"
            :loading="loading"
          >
            刷新
          </el-button>
        </div>
      </template>
      
      <AccountTable
        :accounts="accounts"
        :loading="loading"
        @edit="editAccount"
        @delete="deleteAccount"
      />
    </el-card>

    <!-- 对话框组件 -->
    <AddAccountDialog 
      v-model="dialogs.add" 
      :account="currentAccount"
      @success="loadAccounts" 
    />
    <ManualAddDialog v-model="dialogs.manualAdd" @success="loadAccounts" />
    <QRLoginDialog v-model="dialogs.qrLogin" @success="loadAccounts" />
    <DeleteConfirmDialog 
      v-model="dialogs.deleteConfirm" 
      :account-id="deleteAccountId"
      @success="loadAccounts" 
    />
  </div>
</template>

<style scoped src="./accounts.css"></style>
