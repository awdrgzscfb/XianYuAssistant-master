package com.feijimiao.xianyuassistant.service;

import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryInventoryImportReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryInventoryItemRespDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryInventorySummaryRespDTO;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryItem;

import java.util.List;

public interface AutoDeliveryInventoryService {

    AutoDeliveryInventorySummaryRespDTO importInventory(AutoDeliveryInventoryImportReqDTO reqDTO);

    AutoDeliveryInventorySummaryRespDTO getInventorySummary(Long accountId, String xyGoodsId);

    List<AutoDeliveryInventoryItemRespDTO> getInventoryItems(Long accountId, String xyGoodsId);

    AutoDeliveryInventoryItemRespDTO updatePendingItem(Long accountId, String xyGoodsId, Long itemId, String deliveryContent);

    void deletePendingItem(Long accountId, String xyGoodsId, Long itemId);

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
