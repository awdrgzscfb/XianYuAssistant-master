package com.feijimiao.xianyuassistant.service;

import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryInventoryImportReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryInventorySummaryRespDTO;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryItem;

/**
 * 自动发货库存服务
 */
public interface AutoDeliveryInventoryService {

    AutoDeliveryInventorySummaryRespDTO importInventory(AutoDeliveryInventoryImportReqDTO reqDTO);

    AutoDeliveryInventorySummaryRespDTO getInventorySummary(Long accountId, String xyGoodsId);

    XianyuGoodsAutoDeliveryItem reserveNextItem(Long accountId,
                                                Long xianyuGoodsId,
                                                String xyGoodsId,
                                                Long recordId,
                                                String orderId,
                                                String buyerUserId,
                                                String buyerUserName);

    void markItemUsed(Long itemId);

    void releaseReservedItem(Long itemId);
}
