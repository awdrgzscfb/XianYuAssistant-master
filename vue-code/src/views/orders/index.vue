<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getAccountList } from '@/api/account';
import { getAutoDeliveryRecords, type AutoDeliveryRecordReq, confirmShipment, type ConfirmShipmentReq } from '@/api/auto-delivery-record';
import { showSuccess, showError, showInfo } from '@/utils';
import { ElMessageBox } from 'element-plus';
import type { Account } from '@/types';

const loading = ref(false);
const accounts = ref<Account[]>([]);
const selectedAccountId = ref<number | null>(null);
const deliveryRecords = ref<any[]>([]);
const recordsTotal = ref(0);
const recordsPageNum = ref(1);
const recordsPageSize = ref(20);

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-';
  return time.replace('T', ' ').substring(0, 19);
};

// 加载账号列表
const loadAccounts = async () => {
  try {
    const response = await getAccountList();
    if (response.code === 0 || response.code === 200) {
      accounts.value = response.data?.accounts || [];
      if (accounts.value.length > 0 && !selectedAccountId.value) {
        selectedAccountId.value = accounts.value[0]?.id || null;
        loadDeliveryRecords();
      }
    }
  } catch (error: any) {
    console.error('加载账号列表失败:', error);
  }
};

// 账号变更
const handleAccountChange = () => {
  recordsPageNum.value = 1;
  loadDeliveryRecords();
};

// 加载自动发货记录
const loadDeliveryRecords = async () => {
  if (!selectedAccountId.value) {
    deliveryRecords.value = [];
    recordsTotal.value = 0;
    return;
  }

  loading.value = true;
  try {
    const req: AutoDeliveryRecordReq = {
      xianyuAccountId: selectedAccountId.value,
      // 不传 xyGoodsId，获取所有商品的记录
      pageNum: recordsPageNum.value,
      pageSize: recordsPageSize.value
    };

    const response = await getAutoDeliveryRecords(req);
    if (response.code === 0 || response.code === 200) {
      deliveryRecords.value = response.data?.records || [];
      recordsTotal.value = response.data?.total || 0;
    } else {
      throw new Error(response.msg || '获取记录失败');
    }
  } catch (error: any) {
    console.error('加载自动发货记录失败:', error);
    deliveryRecords.value = [];
    recordsTotal.value = 0;
  } finally {
    loading.value = false;
  }
};

// 记录分页变化
const handlePageChange = (page: number) => {
  recordsPageNum.value = page;
  loadDeliveryRecords();
};

// 记录每页数量变化
const handleSizeChange = (size: number) => {
  recordsPageSize.value = size;
  recordsPageNum.value = 1;
  loadDeliveryRecords();
};

// 获取状态标签类型
const getRecordStatusType = (state: number) => {
  return state === 1 ? 'success' : 'danger';
};

// 获取状态文本
const getRecordStatusText = (state: number) => {
  return state === 1 ? '成功' : '失败';
};

// 确认收货
const handleConfirmShipment = async (record: any) => {
  if (!selectedAccountId.value) {
    showInfo('请先选择账号');
    return;
  }

  if (!record.orderId) {
    showError('该记录没有订单ID，无法确认收货');
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确定要确认收货吗？订单ID: ${record.orderId}`,
      '确认收货',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );

    const req: ConfirmShipmentReq = {
      xianyuAccountId: selectedAccountId.value,
      orderId: record.orderId
    };

    const response = await confirmShipment(req);
    if (response.code === 0 || response.code === 200) {
      showSuccess(response.data || '确认收货成功');
      await loadDeliveryRecords();
    } else {
      // 检查是否是token过期错误
      if (response.msg && response.msg.includes('Token') || response.msg && response.msg.includes('令牌')) {
        throw new Error('Cookie已过期，请重新扫码登录获取新的Cookie');
      }
      throw new Error(response.msg || '确认收货失败');
    }
  } catch (error: any) {
    if (error === 'cancel') {
      return;
    }
    console.error('确认收货失败:', error);
    showError(error.message || '确认收货失败');
  }
};

onMounted(() => {
  loadAccounts();
});
</script>

<template>
  <div class="orders-page">
    <div class="page-header">
      <h1 class="page-title">订单管理</h1>
      <div class="header-actions">
        <span class="account-label">选择闲鱼账号</span>
        <el-select
          v-model="selectedAccountId"
          placeholder="选择账号"
          style="width: 200px"
          @change="handleAccountChange"
        >
          <el-option
            v-for="account in accounts"
            :key="account.id"
            :label="account.accountNote || account.unb"
            :value="account.id"
          />
        </el-select>
      </div>
    </div>

    <el-card class="orders-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">自动发货订单记录</span>
          <span class="card-subtitle">共 {{ recordsTotal }} 条记录</span>
        </div>
      </template>

      <div class="table-wrapper" v-loading="loading">
        <el-table
          :data="deliveryRecords"
          stripe
          style="width: 100%"
        >
          <el-table-column type="index" label="ID" width="60" align="center" />
          <el-table-column prop="goodsTitle" label="商品标题" min-width="200">
            <template #default="{ row }">
              {{ row.goodsTitle || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="orderId" label="订单ID" width="180">
            <template #default="{ row }">
              <span class="order-id">{{ row.orderId || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="buyerUserId" label="买家ID" width="120">
            <template #default="{ row }">
              {{ row.buyerUserId || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="buyerUserName" label="买家名称" width="120">
            <template #default="{ row }">
              {{ row.buyerUserName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="content" label="发货内容" min-width="200">
            <template #default="{ row }">
              <div class="content-text">{{ row.content || '-' }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="state" label="自动发货结果" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="getRecordStatusType(row.state)" size="small">
                {{ getRecordStatusText(row.state) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="orderState" label="确认发货" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.orderState === 1 ? 'success' : 'info'" size="small">
                {{ row.orderState === 1 ? '已确认' : '未确认' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="发货时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-button
                type="primary"
                size="small"
                :disabled="!row.orderId || row.orderState === 1"
                @click="handleConfirmShipment(row)"
              >
                确认收货
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无订单记录" :image-size="80" />
          </template>
        </el-table>

        <div class="pagination-container" v-if="recordsTotal > 0">
          <el-pagination
            v-model:current-page="recordsPageNum"
            v-model:page-size="recordsPageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="recordsTotal"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.orders-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 15px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.account-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.orders-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
}

.card-subtitle {
  font-size: 13px;
  color: #909399;
}

.table-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.order-id {
  font-family: monospace;
  font-size: 13px;
}

.content-text {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  word-break: break-all;
}

.pagination-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 15px 0;
  margin-top: auto;
}
</style>
