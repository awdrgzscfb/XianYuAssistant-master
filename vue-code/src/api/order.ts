import { request } from '@/utils/request';
import type { ApiResponse } from '@/types';

// 确认收货请求参数
export interface ConfirmShipmentRequest {
  xianyuAccountId: number;
  orderId: string;
}

// 确认收货
export function confirmShipment(data: ConfirmShipmentRequest) {
  return request<ApiResponse<string>>({
    url: '/order/confirmShipment',
    method: 'POST',
    data
  });
}
