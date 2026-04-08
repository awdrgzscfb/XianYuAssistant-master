<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessageBox } from 'element-plus';
import { getAccountList } from '@/api/account';
import { getGoodsList, updateAutoDeliveryStatus, type GoodsItemWithConfig } from '@/api/goods';
import {
  getAutoDeliveryConfig,
  getAutoDeliveryInventorySummary,
  importAutoDeliveryInventory,
  saveOrUpdateAutoDeliveryConfig,
  type AutoDeliveryConfig,
  type AutoDeliveryInventorySummary,
  type GetAutoDeliveryConfigReq,
  type SaveAutoDeliveryConfigReq
} from '@/api/auto-delivery-config';
import {
  confirmShipment,
  getAutoDeliveryRecords,
  type AutoDeliveryRecord,
  type AutoDeliveryRecordReq,
  type ConfirmShipmentReq
} from '@/api/auto-delivery-record';
import { showError, showInfo, showSuccess } from '@/utils';
import type { Account } from '@/types';
import GoodsDetailDialog from '../goods/components/GoodsDetailDialog.vue';

const loading = ref(false);
const saving = ref(false);
const importingInventory = ref(false);
const accounts = ref<Account[]>([]);
const selectedAccountId = ref<number | null>(null);
const goodsList = ref<GoodsItemWithConfig[]>([]);
const selectedGoods = ref<GoodsItemWithConfig | null>(null);
const currentConfig = ref<AutoDeliveryConfig | null>(null);

const detailDialogVisible = ref(false);
const selectedGoodsId = ref('');

const configForm = ref({
  type: 1,
  autoDeliveryContent: '',
  autoConfirmShipment: 0
});

const inventoryImportText = ref('');
const replacePendingItems = ref(true);
const inventorySummary = ref<AutoDeliveryInventorySummary>({
  xianyuAccountId: 0,
  xyGoodsId: '',
  totalCount: 0,
  pendingCount: 0,
  usedCount: 0,
  reservedCount: 0,
  importedCount: 0
});

const recordsLoading = ref(false);
const deliveryRecords = ref<AutoDeliveryRecord[]>([]);
const recordsTotal = ref(0);
const recordsPageNum = ref(1);
const recordsPageSize = ref(20);

const resetConfigForm = () => {
  configForm.value = {
    type: 1,
    autoDeliveryContent: '',
    autoConfirmShipment: 0
  };
};

const resetInventorySummary = () => {
  inventorySummary.value = {
    xianyuAccountId: selectedAccountId.value || 0,
    xyGoodsId: selectedGoods.value?.item.xyGoodId || '',
    totalCount: 0,
    pendingCount: 0,
    usedCount: 0,
    reservedCount: 0,
    importedCount: 0
  };
};

const formatTime = (time?: string) => {
  if (!time) return '-';
  return time.replace('T', ' ').substring(0, 19);
};

const formatPrice = (price?: string) => (price ? `¥${price}` : '-');

const getGoodsStatusText = (status: number) => {
  if (status === 0) return '在售';
  if (status === 1) return '已下架';
  if (status === 2) return '已售出';
  return '未知';
};

const getGoodsStatusType = (status: number) => {
  if (status === 0) return 'success';
  if (status === 1) return 'info';
  if (status === 2) return 'warning';
  return 'info';
};

const getRecordStatusText = (state: number) => (state === 1 ? '成功' : '失败');
const getRecordStatusType = (state: number) => (state === 1 ? 'success' : 'danger');

const loadAccounts = async () => {
  try {
    const response = await getAccountList();
    if (response.code === 0 || response.code === 200) {
      accounts.value = response.data?.accounts || [];
      if (!selectedAccountId.value && accounts.value.length > 0) {
        selectedAccountId.value = accounts.value[0]?.id || null;
        await loadGoods();
      }
    }
  } catch (error) {
    console.error('加载账号列表失败:', error);
  }
};

const loadGoods = async () => {
  if (!selectedAccountId.value) {
    return;
  }

  loading.value = true;
  try {
    const response = await getGoodsList({
      xianyuAccountId: selectedAccountId.value,
      pageNum: 1,
      pageSize: 100
    });

    if (response.code === 0 || response.code === 200) {
      goodsList.value = response.data?.itemsWithConfig || [];
      if (goodsList.value.length > 0) {
        await selectGoods(goodsList.value[0]!);
      } else {
        selectedGoods.value = null;
        currentConfig.value = null;
        resetConfigForm();
        resetInventorySummary();
      }
    } else {
      throw new Error(response.msg || '获取商品列表失败');
    }
  } catch (error) {
    console.error('加载商品列表失败:', error);
    goodsList.value = [];
  } finally {
    loading.value = false;
  }
};

const handleAccountChange = async () => {
  selectedGoods.value = null;
  currentConfig.value = null;
  resetConfigForm();
  resetInventorySummary();
  inventoryImportText.value = '';
  await loadGoods();
};

const loadInventorySummary = async () => {
  if (!selectedAccountId.value || !selectedGoods.value) {
    resetInventorySummary();
    return;
  }

  try {
    const response = await getAutoDeliveryInventorySummary({
      xianyuAccountId: selectedAccountId.value,
      xyGoodsId: selectedGoods.value.item.xyGoodId
    });

    if (response.code === 0 || response.code === 200) {
      inventorySummary.value = response.data || {
        xianyuAccountId: selectedAccountId.value,
        xyGoodsId: selectedGoods.value.item.xyGoodId,
        totalCount: 0,
        pendingCount: 0,
        usedCount: 0,
        reservedCount: 0,
        importedCount: 0
      };
    }
  } catch (error) {
    console.error('加载库存汇总失败:', error);
    resetInventorySummary();
  }
};

const loadConfig = async () => {
  if (!selectedAccountId.value || !selectedGoods.value) return;

  try {
    const req: GetAutoDeliveryConfigReq = {
      xianyuAccountId: selectedAccountId.value,
      xyGoodsId: selectedGoods.value.item.xyGoodId
    };
    const response = await getAutoDeliveryConfig(req);
    if (response.code === 0 || response.code === 200) {
      currentConfig.value = response.data || null;
      if (response.data) {
        configForm.value.type = response.data.type ?? 1;
        configForm.value.autoDeliveryContent = response.data.autoDeliveryContent || '';
        configForm.value.autoConfirmShipment = response.data.autoConfirmShipment || 0;
      } else {
        resetConfigForm();
      }
    } else {
      throw new Error(response.msg || '获取配置失败');
    }
  } catch (error) {
    console.error('加载配置失败:', error);
    currentConfig.value = null;
    resetConfigForm();
  } finally {
    await loadInventorySummary();
  }
};

const loadDeliveryRecords = async () => {
  if (!selectedAccountId.value || !selectedGoods.value) {
    deliveryRecords.value = [];
    recordsTotal.value = 0;
    return;
  }

  recordsLoading.value = true;
  try {
    const req: AutoDeliveryRecordReq = {
      xianyuAccountId: selectedAccountId.value,
      xyGoodsId: selectedGoods.value.item.xyGoodId,
      pageNum: recordsPageNum.value,
      pageSize: recordsPageSize.value
    };
    const response = await getAutoDeliveryRecords(req);
    if (response.code === 0 || response.code === 200) {
      deliveryRecords.value = response.data?.records || [];
      recordsTotal.value = response.data?.total || 0;
    } else {
      throw new Error(response.msg || '获取发货记录失败');
    }
  } catch (error) {
    console.error('加载发货记录失败:', error);
    deliveryRecords.value = [];
    recordsTotal.value = 0;
  } finally {
    recordsLoading.value = false;
  }
};

const selectGoods = async (goods: GoodsItemWithConfig) => {
  selectedGoods.value = goods;
  recordsPageNum.value = 1;
  inventoryImportText.value = '';
  await loadConfig();
  await loadDeliveryRecords();
};

const toggleAutoDelivery = async (value: number | string | boolean) => {
  if (!selectedGoods.value || !selectedAccountId.value) {
    return;
  }

  const enabled = Number(value) === 1;
  try {
    const response = await updateAutoDeliveryStatus({
      xianyuAccountId: selectedAccountId.value,
      xyGoodsId: selectedGoods.value.item.xyGoodId,
      xianyuAutoDeliveryOn: enabled ? 1 : 0
    });
    if (response.code === 0 || response.code === 200) {
      selectedGoods.value.xianyuAutoDeliveryOn = enabled ? 1 : 0;
      const target = goodsList.value.find(item => item.item.xyGoodId === selectedGoods.value?.item.xyGoodId);
      if (target) {
        target.xianyuAutoDeliveryOn = enabled ? 1 : 0;
      }
      showSuccess(`自动发货${enabled ? '开启' : '关闭'}成功`);
    } else {
      throw new Error(response.msg || '更新自动发货状态失败');
    }
  } catch (error) {
    console.error('更新自动发货状态失败:', error);
    if (selectedGoods.value) {
      selectedGoods.value.xianyuAutoDeliveryOn = enabled ? 0 : 1;
    }
  }
};

const saveConfig = async () => {
  if (!selectedAccountId.value || !selectedGoods.value) {
    showInfo('请先选择商品');
    return;
  }

  if (configForm.value.type === 1 && !configForm.value.autoDeliveryContent.trim()) {
    showInfo('请输入固定发货内容');
    return;
  }

  if (configForm.value.type === 2 && inventorySummary.value.pendingCount === 0 && !inventoryImportText.value.trim()) {
    showInfo('请先导入至少一条发货库存');
    return;
  }

  saving.value = true;
  try {
    const req: SaveAutoDeliveryConfigReq = {
      xianyuAccountId: selectedAccountId.value,
      xianyuGoodsId: selectedGoods.value.item.id,
      xyGoodsId: selectedGoods.value.item.xyGoodId,
      type: configForm.value.type,
      autoDeliveryContent: configForm.value.type === 1 ? configForm.value.autoDeliveryContent.trim() : '',
      autoConfirmShipment: configForm.value.autoConfirmShipment
    };
    const response = await saveOrUpdateAutoDeliveryConfig(req);
    if (response.code !== 0 && response.code !== 200) {
      throw new Error(response.msg || '保存自动发货配置失败');
    }

    if (configForm.value.type === 2 && inventoryImportText.value.trim()) {
      await doImportInventory();
    } else {
      await loadConfig();
    }

    showSuccess('自动发货配置保存成功');
  } catch (error) {
    console.error('保存配置失败:', error);
  } finally {
    saving.value = false;
  }
};

const doImportInventory = async () => {
  if (!selectedAccountId.value || !selectedGoods.value) {
    showInfo('请先选择商品');
    return;
  }

  if (!inventoryImportText.value.trim()) {
    showInfo('请输入要导入的多行内容');
    return;
  }

  importingInventory.value = true;
  try {
    const response = await importAutoDeliveryInventory({
      xianyuAccountId: selectedAccountId.value,
      xianyuGoodsId: selectedGoods.value.item.id,
      xyGoodsId: selectedGoods.value.item.xyGoodId,
      deliveryItemsText: inventoryImportText.value,
      replacePendingItems: replacePendingItems.value
    });
    if (response.code === 0 || response.code === 200) {
      inventorySummary.value = response.data || inventorySummary.value;
      inventoryImportText.value = '';
      await loadConfig();
      showSuccess(`库存导入成功，本次导入 ${response.data?.importedCount || 0} 条`);
    } else {
      throw new Error(response.msg || '导入库存失败');
    }
  } catch (error) {
    console.error('导入库存失败:', error);
  } finally {
    importingInventory.value = false;
  }
};

const handleRecordsPageChange = async (page: number) => {
  recordsPageNum.value = page;
  await loadDeliveryRecords();
};

const handleRecordsSizeChange = async (size: number) => {
  recordsPageSize.value = size;
  recordsPageNum.value = 1;
  await loadDeliveryRecords();
};

const handleConfirmShipment = async (record: AutoDeliveryRecord) => {
  if (!selectedAccountId.value) {
    showInfo('请先选择账号');
    return;
  }

  if (!record.orderId) {
    showError('该记录没有订单ID，无法确认发货');
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确认发货后将调用闲鱼接口处理订单，订单ID: ${record.orderId}`,
      '确认发货',
      {
        confirmButtonText: '确认',
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
      showSuccess(response.data || '确认发货成功');
      await loadDeliveryRecords();
    } else {
      throw new Error(response.msg || '确认发货失败');
    }
  } catch (error: any) {
    if (error === 'cancel') {
      return;
    }
    console.error('确认发货失败:', error);
    showError(error.message || '确认发货失败');
  }
};

const openGoodsDetail = () => {
  if (!selectedGoods.value) {
    return;
  }
  selectedGoodsId.value = selectedGoods.value.item.xyGoodId;
  detailDialogVisible.value = true;
};

onMounted(() => {
  loadAccounts();
});
</script>

<template>
  <div class="auto-delivery-page">
    <div class="page-header">
      <h1 class="page-title">自动发货配置</h1>
      <div class="header-actions">
        <span class="account-label">闲鱼账号</span>
        <el-select
          v-model="selectedAccountId"
          placeholder="选择账号"
          style="width: 220px"
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

    <div class="layout">
      <el-card class="goods-panel">
        <template #header>
          <div class="card-header">
            <span>商品列表</span>
            <span class="sub-text">共 {{ goodsList.length }} 件</span>
          </div>
        </template>

        <div class="goods-list" v-loading="loading">
          <div
            v-for="goods in goodsList"
            :key="goods.item.xyGoodId"
            class="goods-item"
            :class="{ active: selectedGoods?.item.xyGoodId === goods.item.xyGoodId }"
            @click="selectGoods(goods)"
          >
            <el-image :src="goods.item.coverPic" fit="cover" class="goods-image" />
            <div class="goods-info">
              <div class="goods-title">{{ goods.item.title }}</div>
              <div class="goods-meta">
                <span class="goods-price">{{ formatPrice(goods.item.soldPrice) }}</span>
                <el-tag :type="getGoodsStatusType(goods.item.status)" size="small">
                  {{ getGoodsStatusText(goods.item.status) }}
                </el-tag>
              </div>
            </div>
          </div>

          <el-empty v-if="!loading && goodsList.length === 0" description="暂无商品" />
        </div>
      </el-card>

      <el-card class="config-panel">
        <template #header>
          <div class="card-header">
            <span>发货策略</span>
            <span v-if="selectedGoods" class="sub-text">{{ selectedGoods.item.xyGoodId }}</span>
          </div>
        </template>

        <div v-if="selectedGoods" class="config-body">
          <div class="title-row">
            <div class="selected-title">{{ selectedGoods.item.title }}</div>
            <el-button type="primary" size="small" @click="openGoodsDetail">查看详情</el-button>
          </div>

          <el-form :model="configForm" label-width="110px">
            <el-form-item label="自动发货开关">
              <el-switch
                v-model="selectedGoods.xianyuAutoDeliveryOn"
                :active-value="1"
                :inactive-value="0"
                @change="toggleAutoDelivery"
              />
              <span class="inline-tip">
                {{ selectedGoods.xianyuAutoDeliveryOn === 1 ? '已开启' : '已关闭' }}
              </span>
            </el-form-item>

            <el-form-item label="发货模式">
              <el-radio-group v-model="configForm.type">
                <el-radio :value="1">固定文本</el-radio>
                <el-radio :value="2">逐条库存发货</el-radio>
              </el-radio-group>
            </el-form-item>

            <template v-if="configForm.type === 1">
              <el-form-item label="固定发货内容">
                <el-input
                  v-model="configForm.autoDeliveryContent"
                  type="textarea"
                  :rows="8"
                  maxlength="2000"
                  show-word-limit
                  placeholder="买家付款后，将自动发送这里的固定文本"
                />
              </el-form-item>
            </template>

            <template v-else>
              <el-form-item label="库存状态">
                <div class="inventory-summary">
                  <el-tag type="primary">总数 {{ inventorySummary.totalCount }}</el-tag>
                  <el-tag type="success">待发 {{ inventorySummary.pendingCount }}</el-tag>
                  <el-tag type="info">已用 {{ inventorySummary.usedCount }}</el-tag>
                  <el-tag type="warning">预占 {{ inventorySummary.reservedCount }}</el-tag>
                </div>
              </el-form-item>

              <el-form-item label="多行导入">
                <el-input
                  v-model="inventoryImportText"
                  type="textarea"
                  :rows="10"
                  maxlength="20000"
                  show-word-limit
                  placeholder="一行一条发货内容。每次买家付款后自动取一行发送，发送成功后该行会被消耗。"
                />
              </el-form-item>

              <el-form-item label="导入方式">
                <el-checkbox v-model="replacePendingItems">
                  覆盖当前未使用库存
                </el-checkbox>
                <span class="form-tip">
                  不勾选时为追加导入；已使用库存不会被删除。
                </span>
              </el-form-item>

              <el-form-item>
                <el-button type="success" :loading="importingInventory" @click="doImportInventory">
                  导入库存
                </el-button>
              </el-form-item>
            </template>

            <el-form-item label="自动确认发货">
              <el-switch
                v-model="configForm.autoConfirmShipment"
                :active-value="1"
                :inactive-value="0"
              />
              <span class="inline-tip">
                {{ configForm.autoConfirmShipment === 1 ? '自动确认' : '不自动确认' }}
              </span>
            </el-form-item>

            <el-form-item>
              <div class="action-row">
                <el-button type="primary" :loading="saving" @click="saveConfig">保存配置</el-button>
                <span v-if="currentConfig" class="sub-text">
                  上次更新 {{ formatTime(currentConfig.updateTime) }}
                </span>
              </div>
            </el-form-item>
          </el-form>

          <div class="records-section">
            <div class="records-header">
              <span class="records-title">自动发货记录</span>
              <span class="sub-text">共 {{ recordsTotal }} 条</span>
            </div>

            <el-table :data="deliveryRecords" stripe v-loading="recordsLoading" style="width: 100%">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="orderId" label="订单ID" width="180" />
              <el-table-column prop="buyerUserName" label="买家" width="120" />
              <el-table-column prop="content" label="发货内容" min-width="220" show-overflow-tooltip />
              <el-table-column label="结果" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="getRecordStatusType(row.state)" size="small">
                    {{ getRecordStatusText(row.state) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="确认发货" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.orderState === 1 ? 'success' : 'info'" size="small">
                    {{ row.orderState === 1 ? '已确认' : '未确认' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="时间" width="180">
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
                    确认发货
                  </el-button>
                </template>
              </el-table-column>

              <template #empty>
                <el-empty description="暂无发货记录" :image-size="80" />
              </template>
            </el-table>

            <div class="pagination-row" v-if="recordsTotal > 0">
              <el-pagination
                v-model:current-page="recordsPageNum"
                v-model:page-size="recordsPageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="recordsTotal"
                layout="total, sizes, prev, pager, next"
                @size-change="handleRecordsSizeChange"
                @current-change="handleRecordsPageChange"
              />
            </div>
          </div>
        </div>

        <el-empty v-else description="请先选择商品" />
      </el-card>
    </div>

    <GoodsDetailDialog
      v-model="detailDialogVisible"
      :goods-id="selectedGoodsId"
      :account-id="selectedAccountId"
    />
  </div>
</template>

<style scoped>
.auto-delivery-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.account-label,
.sub-text,
.inline-tip,
.form-tip {
  color: #909399;
  font-size: 13px;
}

.layout {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 16px;
}

.goods-panel,
.config-panel {
  min-height: 0;
}

.goods-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: calc(100vh - 240px);
  overflow: auto;
}

.goods-item {
  display: flex;
  gap: 12px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.goods-item:hover {
  border-color: #c6e2ff;
  background: #f8fbff;
}

.goods-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.goods-image {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  flex-shrink: 0;
}

.goods-info {
  min-width: 0;
  flex: 1;
}

.goods-title {
  font-size: 14px;
  color: #303133;
  line-height: 1.4;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.goods-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.goods-price {
  color: #f56c6c;
  font-weight: 600;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.config-body {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.selected-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  line-height: 1.5;
}

.inventory-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.action-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.records-section {
  border-top: 1px solid #ebeef5;
  padding-top: 20px;
}

.records-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.records-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.pagination-row {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 980px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .goods-list {
    max-height: none;
  }
}
</style>
