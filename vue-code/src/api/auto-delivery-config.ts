import { request } from '@/utils/request';

export interface AutoDeliveryConfig {
  id: number;
  xianyuAccountId: number;
  xianyuGoodsId: number;
  xyGoodsId: string;
  type: number;
  autoDeliveryContent: string;
  autoConfirmShipment?: number;
  totalItemCount?: number;
  pendingItemCount?: number;
  usedItemCount?: number;
  reservedItemCount?: number;
  createTime: string;
  updateTime: string;
}

export interface SaveAutoDeliveryConfigReq {
  xianyuAccountId: number;
  xianyuGoodsId?: number;
  xyGoodsId: string;
  type: number;
  autoDeliveryContent: string;
  autoConfirmShipment?: number;
}

export interface GetAutoDeliveryConfigReq {
  xianyuAccountId: number;
  xyGoodsId?: string;
}

export interface AutoDeliveryInventoryImportReq {
  xianyuAccountId: number;
  xianyuGoodsId?: number;
  xyGoodsId: string;
  deliveryItemsText: string;
  replacePendingItems?: boolean;
}

export interface AutoDeliveryInventorySummary {
  xianyuAccountId: number;
  xyGoodsId: string;
  totalCount: number;
  pendingCount: number;
  usedCount: number;
  reservedCount: number;
  importedCount: number;
}

export function saveOrUpdateAutoDeliveryConfig(data: SaveAutoDeliveryConfigReq) {
  return request<AutoDeliveryConfig>({
    url: '/auto-delivery-config/save',
    method: 'POST',
    data
  });
}

export function getAutoDeliveryConfig(data: GetAutoDeliveryConfigReq) {
  return request<AutoDeliveryConfig>({
    url: '/auto-delivery-config/get',
    method: 'POST',
    data
  });
}

export function importAutoDeliveryInventory(data: AutoDeliveryInventoryImportReq) {
  return request<AutoDeliveryInventorySummary>({
    url: '/auto-delivery-config/inventory/import',
    method: 'POST',
    data
  });
}

export function getAutoDeliveryInventorySummary(data: GetAutoDeliveryConfigReq) {
  return request<AutoDeliveryInventorySummary>({
    url: '/auto-delivery-config/inventory/summary',
    method: 'POST',
    data
  });
}

export function getAutoDeliveryConfigsByAccountId(xianyuAccountId: number) {
  return request<AutoDeliveryConfig[]>({
    url: '/auto-delivery-config/list',
    method: 'POST',
    params: { xianyuAccountId }
  });
}

export function deleteAutoDeliveryConfig(xianyuAccountId: number, xyGoodsId: string) {
  return request({
    url: '/auto-delivery-config/delete',
    method: 'POST',
    params: { xianyuAccountId, xyGoodsId }
  });
}
