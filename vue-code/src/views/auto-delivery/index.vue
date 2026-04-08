<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessageBox } from 'element-plus';
import { getAccountList } from '@/api/account';
import { getGoodsList, updateAutoDeliveryStatus, type GoodsItemWithConfig } from '@/api/goods';
import {
  deleteAutoDeliveryInventoryItem,
  getAutoDeliveryConfig,
  getAutoDeliveryInventoryItems,
  getAutoDeliveryInventorySummary,
  importAutoDeliveryInventory,
  saveOrUpdateAutoDeliveryConfig,
  updateAutoDeliveryInventoryItem,
  type AutoDeliveryConfig,
  type AutoDeliveryInventoryItem,
  type AutoDeliveryInventorySummary
} from '@/api/auto-delivery-config';
import { confirmShipment, getAutoDeliveryRecords, type AutoDeliveryRecord } from '@/api/auto-delivery-record';
import { showError, showInfo, showSuccess } from '@/utils';
import type { Account } from '@/types';
import GoodsDetailDialog from '../goods/components/GoodsDetailDialog.vue';

const loading = ref(false);
const saving = ref(false);
const importingInventory = ref(false);
const inventoryItemsLoading = ref(false);
const inventoryItemUpdating = ref(false);
const accounts = ref<Account[]>([]);
const selectedAccountId = ref<number | null>(null);
const goodsList = ref<GoodsItemWithConfig[]>([]);
const selectedGoods = ref<GoodsItemWithConfig | null>(null);
const currentConfig = ref<AutoDeliveryConfig | null>(null);
const detailDialogVisible = ref(false);
const selectedGoodsId = ref('');
const configForm = ref({ type: 1, autoDeliveryContent: '', autoConfirmShipment: 0 });
const inventoryImportText = ref('');
const replacePendingItems = ref(true);
const inventorySummary = ref<AutoDeliveryInventorySummary>(emptySummary());
const inventoryItems = ref<AutoDeliveryInventoryItem[]>([]);
const inventoryEditDialogVisible = ref(false);
const inventoryEditForm = ref({ itemId: 0, deliveryContent: '' });
const recordsLoading = ref(false);
const deliveryRecords = ref<AutoDeliveryRecord[]>([]);
const recordsTotal = ref(0);
const recordsPageNum = ref(1);
const recordsPageSize = ref(20);

function emptySummary(): AutoDeliveryInventorySummary {
  return {
    xianyuAccountId: selectedAccountId.value || 0,
    xyGoodsId: selectedGoods.value?.item.xyGoodId || '',
    totalCount: 0,
    pendingCount: 0,
    usedCount: 0,
    reservedCount: 0,
    importedCount: 0
  };
}

const formatTime = (time?: string) => (time ? time.replace('T', ' ').substring(0, 19) : '-');
const formatPrice = (price?: string) => (price ? `¥${price}` : '-');
const goodsStatusText = (s: number) => (s === 0 ? '在售' : s === 1 ? '已下架' : s === 2 ? '已售出' : '未知');
const goodsStatusType = (s: number) => (s === 0 ? 'success' : s === 1 ? 'info' : s === 2 ? 'warning' : 'info');
const recordStatusText = (s: number) => (s === 1 ? '成功' : '失败');
const recordStatusType = (s: number) => (s === 1 ? 'success' : 'danger');
const itemStateText = (s: number) => (s === 0 ? '待发' : s === 1 ? '已使用' : s === 2 ? '预占中' : '未知');
const itemStateType = (s: number) => (s === 0 ? 'success' : s === 1 ? 'info' : s === 2 ? 'warning' : 'info');
const canEditItem = (item: AutoDeliveryInventoryItem) => item.state === 0;

const resetConfig = () => {
  configForm.value = { type: 1, autoDeliveryContent: '', autoConfirmShipment: 0 };
};

const resetInventory = () => {
  inventorySummary.value = emptySummary();
  inventoryItems.value = [];
  inventoryImportText.value = '';
};

const goodsReq = () => {
  if (!selectedAccountId.value || !selectedGoods.value) return null;
  return { xianyuAccountId: selectedAccountId.value, xyGoodsId: selectedGoods.value.item.xyGoodId };
};

const loadAccounts = async () => {
  try {
    const res = await getAccountList();
    if (res.code === 0 || res.code === 200) {
      accounts.value = res.data?.accounts || [];
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
  if (!selectedAccountId.value) return;
  loading.value = true;
  try {
    const res = await getGoodsList({ xianyuAccountId: selectedAccountId.value, pageNum: 1, pageSize: 100 });
    if (res.code !== 0 && res.code !== 200) throw new Error(res.msg || '获取商品列表失败');
    goodsList.value = res.data?.itemsWithConfig || [];
    if (goodsList.value.length > 0) {
      await selectGoods(goodsList.value[0]!);
    } else {
      selectedGoods.value = null;
      currentConfig.value = null;
      deliveryRecords.value = [];
      recordsTotal.value = 0;
      resetConfig();
      resetInventory();
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
  deliveryRecords.value = [];
  recordsTotal.value = 0;
  resetConfig();
  resetInventory();
  await loadGoods();
};

const loadConfig = async () => {
  const req = goodsReq();
  if (!req) return;
  try {
    const res = await getAutoDeliveryConfig(req);
    if (res.code !== 0 && res.code !== 200) throw new Error(res.msg || '获取配置失败');
    currentConfig.value = res.data || null;
    if (res.data) {
      configForm.value.type = res.data.type ?? 1;
      configForm.value.autoDeliveryContent = res.data.autoDeliveryContent || '';
      configForm.value.autoConfirmShipment = res.data.autoConfirmShipment || 0;
    } else {
      resetConfig();
    }
  } catch (error) {
    console.error('加载配置失败:', error);
    currentConfig.value = null;
    resetConfig();
  }
};

const loadInventorySummary = async () => {
  const req = goodsReq();
  if (!req) return void (inventorySummary.value = emptySummary());
  try {
    const res = await getAutoDeliveryInventorySummary(req);
    if (res.code === 0 || res.code === 200) inventorySummary.value = res.data || emptySummary();
  } catch (error) {
    console.error('加载库存汇总失败:', error);
    inventorySummary.value = emptySummary();
  }
};

const loadInventoryItems = async () => {
  const req = goodsReq();
  if (!req) return void (inventoryItems.value = []);
  inventoryItemsLoading.value = true;
  try {
    const res = await getAutoDeliveryInventoryItems(req);
    if (res.code !== 0 && res.code !== 200) throw new Error(res.msg || '获取库存明细失败');
    inventoryItems.value = res.data || [];
  } catch (error) {
    console.error('加载库存明细失败:', error);
    inventoryItems.value = [];
  } finally {
    inventoryItemsLoading.value = false;
  }
};

const refreshInventory = async () => {
  await loadInventorySummary();
  await loadInventoryItems();
};

const loadRecords = async () => {
  const req = goodsReq();
  if (!req) {
    deliveryRecords.value = [];
    recordsTotal.value = 0;
    return;
  }
  recordsLoading.value = true;
  try {
    const res = await getAutoDeliveryRecords({
      xianyuAccountId: req.xianyuAccountId,
      xyGoodsId: req.xyGoodsId,
      pageNum: recordsPageNum.value,
      pageSize: recordsPageSize.value
    });
    if (res.code !== 0 && res.code !== 200) throw new Error(res.msg || '获取发货记录失败');
    deliveryRecords.value = res.data?.records || [];
    recordsTotal.value = res.data?.total || 0;
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
  inventoryEditDialogVisible.value = false;
  resetInventory();
  await loadConfig();
  await refreshInventory();
  await loadRecords();
};

const toggleAutoDelivery = async (value: number | string | boolean) => {
  if (!selectedGoods.value || !selectedAccountId.value) return;
  const enabled = Number(value) === 1;
  try {
    const res = await updateAutoDeliveryStatus({
      xianyuAccountId: selectedAccountId.value,
      xyGoodsId: selectedGoods.value.item.xyGoodId,
      xianyuAutoDeliveryOn: enabled ? 1 : 0
    });
    if (res.code !== 0 && res.code !== 200) throw new Error(res.msg || '更新自动发货状态失败');
    selectedGoods.value.xianyuAutoDeliveryOn = enabled ? 1 : 0;
    const target = goodsList.value.find(item => item.item.xyGoodId === selectedGoods.value?.item.xyGoodId);
    if (target) target.xianyuAutoDeliveryOn = enabled ? 1 : 0;
    showSuccess(`自动发货${enabled ? '开启' : '关闭'}成功`);
  } catch (error) {
    console.error('更新自动发货状态失败:', error);
    if (selectedGoods.value) selectedGoods.value.xianyuAutoDeliveryOn = enabled ? 0 : 1;
  }
};

const saveConfig = async () => {
  if (!selectedAccountId.value || !selectedGoods.value) return showInfo('请先选择商品');
  if (configForm.value.type === 1 && !configForm.value.autoDeliveryContent.trim()) return showInfo('请输入固定发货内容');
  if (configForm.value.type === 2 && inventorySummary.value.pendingCount === 0 && !inventoryImportText.value.trim()) return showInfo('请先导入至少一条发货库存');
  saving.value = true;
  try {
    const res = await saveOrUpdateAutoDeliveryConfig({
      xianyuAccountId: selectedAccountId.value,
      xianyuGoodsId: selectedGoods.value.item.id,
      xyGoodsId: selectedGoods.value.item.xyGoodId,
      type: configForm.value.type,
      autoDeliveryContent: configForm.value.type === 1 ? configForm.value.autoDeliveryContent.trim() : '',
      autoConfirmShipment: configForm.value.autoConfirmShipment
    });
    if (res.code !== 0 && res.code !== 200) throw new Error(res.msg || '保存自动发货配置失败');
    if (configForm.value.type === 2 && inventoryImportText.value.trim()) await importInventory();
    else {
      await loadConfig();
      await refreshInventory();
    }
    showSuccess('自动发货配置保存成功');
  } catch (error) {
    console.error('保存配置失败:', error);
  } finally {
    saving.value = false;
  }
};

const importInventory = async () => {
  if (!selectedAccountId.value || !selectedGoods.value) return showInfo('请先选择商品');
  if (!inventoryImportText.value.trim()) return showInfo('请输入要导入的多行内容');
  importingInventory.value = true;
  try {
    const res = await importAutoDeliveryInventory({
      xianyuAccountId: selectedAccountId.value,
      xianyuGoodsId: selectedGoods.value.item.id,
      xyGoodsId: selectedGoods.value.item.xyGoodId,
      deliveryItemsText: inventoryImportText.value,
      replacePendingItems: replacePendingItems.value
    });
    if (res.code !== 0 && res.code !== 200) throw new Error(res.msg || '导入库存失败');
    inventorySummary.value = res.data || inventorySummary.value;
    inventoryImportText.value = '';
    await loadConfig();
    await loadInventoryItems();
    showSuccess(`库存导入成功，本次导入 ${res.data?.importedCount || 0} 条`);
  } catch (error) {
    console.error('导入库存失败:', error);
  } finally {
    importingInventory.value = false;
  }
};

const openEditItem = (item: AutoDeliveryInventoryItem) => {
  inventoryEditForm.value = { itemId: item.id, deliveryContent: item.deliveryContent };
  inventoryEditDialogVisible.value = true;
};

const submitInventoryEdit = async () => {
  if (!selectedAccountId.value || !selectedGoods.value) return showInfo('请先选择商品');
  if (!inventoryEditForm.value.deliveryContent.trim()) return showInfo('发货内容不能为空');
  inventoryItemUpdating.value = true;
  try {
    const res = await updateAutoDeliveryInventoryItem({
      xianyuAccountId: selectedAccountId.value,
      xyGoodsId: selectedGoods.value.item.xyGoodId,
      itemId: inventoryEditForm.value.itemId,
      deliveryContent: inventoryEditForm.value.deliveryContent.trim()
    });
    if (res.code !== 0 && res.code !== 200) throw new Error(res.msg || '修改库存失败');
    inventoryEditDialogVisible.value = false;
    await loadInventoryItems();
    showSuccess('库存内容修改成功');
  } catch (error) {
    console.error('修改库存失败:', error);
  } finally {
    inventoryItemUpdating.value = false;
  }
};

const deleteInventoryItem = async (item: AutoDeliveryInventoryItem) => {
  if (!selectedAccountId.value || !selectedGoods.value) return showInfo('请先选择商品');
  try {
    await ElMessageBox.confirm(`确认删除库存项 #${item.id} 吗？删除后不可恢复。`, '删除库存项', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    const res = await deleteAutoDeliveryInventoryItem({
      xianyuAccountId: selectedAccountId.value,
      xyGoodsId: selectedGoods.value.item.xyGoodId,
      itemId: item.id
    });
    if (res.code !== 0 && res.code !== 200) throw new Error(res.msg || '删除库存失败');
    await refreshInventory();
    showSuccess('库存项删除成功');
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return;
    console.error('删除库存失败:', error);
    showError(error.message || '删除库存失败');
  }
};

const handleConfirmShipment = async (record: AutoDeliveryRecord) => {
  if (!selectedAccountId.value) return showInfo('请先选择账号');
  if (!record.orderId) return showError('该记录没有订单ID，无法确认发货');
  try {
    await ElMessageBox.confirm(`确认发货后将调用闲鱼接口处理订单，订单ID: ${record.orderId}`, '确认发货', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    });
    const res = await confirmShipment({ xianyuAccountId: selectedAccountId.value, orderId: record.orderId });
    if (res.code !== 0 && res.code !== 200) throw new Error(res.msg || '确认发货失败');
    showSuccess(res.data || '确认发货成功');
    await loadRecords();
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return;
    console.error('确认发货失败:', error);
    showError(error.message || '确认发货失败');
  }
};

const openGoodsDetail = () => {
  if (!selectedGoods.value) return;
  selectedGoodsId.value = selectedGoods.value.item.xyGoodId;
  detailDialogVisible.value = true;
};

onMounted(loadAccounts);
</script>

<template>
  <div class="auto-delivery-page">
    <div class="page-header">
      <h1 class="page-title">自动发货配置</h1>
      <div class="header-actions">
        <span class="sub-text">闲鱼账号</span>
        <el-select v-model="selectedAccountId" placeholder="选择账号" style="width: 220px" @change="handleAccountChange">
          <el-option v-for="account in accounts" :key="account.id" :label="account.accountNote || account.unb" :value="account.id" />
        </el-select>
      </div>
    </div>

    <div class="layout">
      <el-card class="goods-panel">
        <template #header><div class="card-header"><span>商品列表</span><span class="sub-text">共 {{ goodsList.length }} 件</span></div></template>
        <div class="goods-list" v-loading="loading">
          <div v-for="goods in goodsList" :key="goods.item.xyGoodId" class="goods-item" :class="{ active: selectedGoods?.item.xyGoodId === goods.item.xyGoodId }" @click="selectGoods(goods)">
            <el-image :src="goods.item.coverPic" fit="cover" class="goods-image" />
            <div class="goods-info">
              <div class="goods-title">{{ goods.item.title }}</div>
              <div class="goods-meta">
                <span class="goods-price">{{ formatPrice(goods.item.soldPrice) }}</span>
                <el-tag :type="goodsStatusType(goods.item.status)" size="small">{{ goodsStatusText(goods.item.status) }}</el-tag>
              </div>
            </div>
          </div>
          <el-empty v-if="!loading && goodsList.length === 0" description="暂无商品" />
        </div>
      </el-card>

      <el-card class="config-panel">
        <template #header><div class="card-header"><span>发货策略</span><span v-if="selectedGoods" class="sub-text">{{ selectedGoods.item.xyGoodId }}</span></div></template>
        <div v-if="selectedGoods" class="config-body">
          <div class="title-row">
            <div class="selected-title">{{ selectedGoods.item.title }}</div>
            <el-button type="primary" size="small" @click="openGoodsDetail">查看详情</el-button>
          </div>

          <el-form :model="configForm" label-width="110px">
            <el-form-item label="自动发货开关">
              <el-switch v-model="selectedGoods.xianyuAutoDeliveryOn" :active-value="1" :inactive-value="0" @change="toggleAutoDelivery" />
              <span class="inline-tip">{{ selectedGoods.xianyuAutoDeliveryOn === 1 ? '已开启' : '已关闭' }}</span>
            </el-form-item>
            <el-form-item label="发货模式">
              <el-radio-group v-model="configForm.type">
                <el-radio :value="1">固定文本</el-radio>
                <el-radio :value="2">逐条库存发货</el-radio>
              </el-radio-group>
            </el-form-item>

            <template v-if="configForm.type === 1">
              <el-form-item label="固定发货内容">
                <el-input v-model="configForm.autoDeliveryContent" type="textarea" :rows="8" maxlength="2000" show-word-limit placeholder="买家付款后，将自动发送这里的固定文本" />
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
                <el-input v-model="inventoryImportText" type="textarea" :rows="8" maxlength="20000" show-word-limit placeholder="一行一条发货内容。每次买家付款后自动取一行发送，发送成功后该行会被消耗。" />
              </el-form-item>
              <el-form-item label="导入方式">
                <el-checkbox v-model="replacePendingItems">覆盖当前未使用库存</el-checkbox>
                <span class="inline-tip">不勾选时为追加导入；已使用库存不会被删除。</span>
              </el-form-item>
              <el-form-item><el-button type="success" :loading="importingInventory" @click="importInventory">导入库存</el-button></el-form-item>

              <div class="section-box">
                <div class="card-header section-head"><span>库存明细</span><span class="sub-text">仅待发状态支持编辑和删除</span></div>
                <el-table :data="inventoryItems" stripe v-loading="inventoryItemsLoading" max-height="360">
                  <el-table-column prop="id" label="ID" width="70" />
                  <el-table-column prop="deliveryContent" label="发货内容" min-width="240" show-overflow-tooltip />
                  <el-table-column label="状态" width="100" align="center">
                    <template #default="{ row }"><el-tag :type="itemStateType(row.state)" size="small">{{ itemStateText(row.state) }}</el-tag></template>
                  </el-table-column>
                  <el-table-column prop="buyerUserName" label="买家" width="120"><template #default="{ row }">{{ row.buyerUserName || '-' }}</template></el-table-column>
                  <el-table-column prop="orderId" label="订单ID" width="170"><template #default="{ row }">{{ row.orderId || '-' }}</template></el-table-column>
                  <el-table-column label="时间" width="170"><template #default="{ row }">{{ formatTime(row.usedTime || row.reservedTime || row.createTime) }}</template></el-table-column>
                  <el-table-column label="操作" width="150" align="center" fixed="right">
                    <template #default="{ row }">
                      <el-button type="primary" text size="small" :disabled="!canEditItem(row)" @click="openEditItem(row)">编辑</el-button>
                      <el-button type="danger" text size="small" :disabled="!canEditItem(row)" @click="deleteInventoryItem(row)">删除</el-button>
                    </template>
                  </el-table-column>
                  <template #empty><el-empty description="暂无库存明细" :image-size="80" /></template>
                </el-table>
              </div>
            </template>

            <el-form-item label="自动确认发货">
              <el-switch v-model="configForm.autoConfirmShipment" :active-value="1" :inactive-value="0" />
              <span class="inline-tip">{{ configForm.autoConfirmShipment === 1 ? '自动确认' : '不自动确认' }}</span>
            </el-form-item>
            <el-form-item>
              <div class="action-row">
                <el-button type="primary" :loading="saving" @click="saveConfig">保存配置</el-button>
                <span v-if="currentConfig" class="sub-text">上次更新 {{ formatTime(currentConfig.updateTime) }}</span>
              </div>
            </el-form-item>
          </el-form>

          <div class="section-box">
            <div class="card-header section-head"><span>自动发货记录</span><span class="sub-text">共 {{ recordsTotal }} 条</span></div>
            <el-table :data="deliveryRecords" stripe v-loading="recordsLoading">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="orderId" label="订单ID" width="180" />
              <el-table-column prop="buyerUserName" label="买家" width="120" />
              <el-table-column prop="content" label="发货内容" min-width="220" show-overflow-tooltip />
              <el-table-column label="结果" width="100" align="center"><template #default="{ row }"><el-tag :type="recordStatusType(row.state)" size="small">{{ recordStatusText(row.state) }}</el-tag></template></el-table-column>
              <el-table-column label="确认发货" width="100" align="center"><template #default="{ row }"><el-tag :type="row.orderState === 1 ? 'success' : 'info'" size="small">{{ row.orderState === 1 ? '已确认' : '未确认' }}</el-tag></template></el-table-column>
              <el-table-column label="时间" width="180"><template #default="{ row }">{{ formatTime(row.createTime) }}</template></el-table-column>
              <el-table-column label="操作" width="120" align="center" fixed="right">
                <template #default="{ row }"><el-button type="primary" size="small" :disabled="!row.orderId || row.orderState === 1" @click="handleConfirmShipment(row)">确认发货</el-button></template>
              </el-table-column>
              <template #empty><el-empty description="暂无发货记录" :image-size="80" /></template>
            </el-table>
            <div class="pagination-row" v-if="recordsTotal > 0">
              <el-pagination v-model:current-page="recordsPageNum" v-model:page-size="recordsPageSize" :page-sizes="[10, 20, 50, 100]" :total="recordsTotal" layout="total, sizes, prev, pager, next" @size-change="loadRecords" @current-change="loadRecords" />
            </div>
          </div>
        </div>
        <el-empty v-else description="请先选择商品" />
      </el-card>
    </div>

    <el-dialog v-model="inventoryEditDialogVisible" title="编辑库存内容" width="640px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="库存项ID"><span>{{ inventoryEditForm.itemId || '-' }}</span></el-form-item>
        <el-form-item label="发货内容"><el-input v-model="inventoryEditForm.deliveryContent" type="textarea" :rows="8" maxlength="5000" show-word-limit placeholder="请输入新的发货内容" /></el-form-item>
      </el-form>
      <template #footer><div class="action-row"><el-button @click="inventoryEditDialogVisible = false">取消</el-button><el-button type="primary" :loading="inventoryItemUpdating" @click="submitInventoryEdit">保存</el-button></div></template>
    </el-dialog>

    <GoodsDetailDialog v-model="detailDialogVisible" :goods-id="selectedGoodsId" :account-id="selectedAccountId" />
  </div>
</template>

<style scoped>
.auto-delivery-page{height:100%;display:flex;flex-direction:column;gap:16px}
.page-header,.header-actions,.card-header,.title-row,.action-row{display:flex;align-items:center;gap:12px}
.page-header,.card-header,.title-row{justify-content:space-between}
.page-title{margin:0;font-size:24px;font-weight:600;color:#303133}
.sub-text,.inline-tip{color:#909399;font-size:13px}
.inline-tip{margin-left:12px}
.layout{flex:1;min-height:0;display:grid;grid-template-columns:360px minmax(0,1fr);gap:16px}
.goods-panel,.config-panel{min-height:0}
.goods-list{display:flex;flex-direction:column;gap:10px;max-height:calc(100vh - 240px);overflow:auto}
.goods-item{display:flex;gap:12px;padding:10px;border:1px solid #ebeef5;border-radius:8px;cursor:pointer;transition:border-color .2s ease,background-color .2s ease}
.goods-item:hover{border-color:#c6e2ff;background:#f8fbff}
.goods-item.active{border-color:#409eff;background:#ecf5ff}
.goods-image{width:56px;height:56px;border-radius:6px;flex-shrink:0}
.goods-info{min-width:0;flex:1}
.goods-title{font-size:14px;color:#303133;line-height:1.4;margin-bottom:8px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.goods-meta,.inventory-summary{display:flex;align-items:center;gap:8px;flex-wrap:wrap}
.goods-meta{justify-content:space-between}
.goods-price{color:#f56c6c;font-weight:600}
.config-body{display:flex;flex-direction:column;gap:24px}
.selected-title{font-size:16px;font-weight:600;color:#303133;line-height:1.5}
.section-box{border-top:1px solid #ebeef5;padding-top:20px}
.section-head{margin-bottom:12px}
.pagination-row{margin-top:16px;display:flex;justify-content:flex-end}
@media (max-width:980px){.layout{grid-template-columns:1fr}.goods-list{max-height:none}.card-header,.title-row{align-items:flex-start;flex-direction:column}}
</style>
