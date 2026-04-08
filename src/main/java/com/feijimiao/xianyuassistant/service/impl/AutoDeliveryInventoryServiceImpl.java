package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryInventoryImportReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryInventorySummaryRespDTO;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryItem;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoDeliveryItemMapper;
import com.feijimiao.xianyuassistant.service.AutoDeliveryInventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动发货库存服务实现
 */
@Slf4j
@Service
public class AutoDeliveryInventoryServiceImpl implements AutoDeliveryInventoryService {

    @Autowired
    private XianyuGoodsAutoDeliveryItemMapper itemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AutoDeliveryInventorySummaryRespDTO importInventory(AutoDeliveryInventoryImportReqDTO reqDTO) {
        List<String> lines = parseLines(reqDTO.getDeliveryItemsText());
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("导入内容不能为空");
        }

        if (Boolean.TRUE.equals(reqDTO.getReplacePendingItems())) {
            itemMapper.deletePendingItems(reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
        }

        List<XianyuGoodsAutoDeliveryItem> items = new ArrayList<>();
        for (String line : lines) {
            XianyuGoodsAutoDeliveryItem item = new XianyuGoodsAutoDeliveryItem();
            item.setXianyuAccountId(reqDTO.getXianyuAccountId());
            item.setXianyuGoodsId(reqDTO.getXianyuGoodsId());
            item.setXyGoodsId(reqDTO.getXyGoodsId());
            item.setDeliveryContent(line);
            item.setState(0);
            items.add(item);
        }

        itemMapper.insertBatch(items);

        AutoDeliveryInventorySummaryRespDTO summary = getInventorySummary(reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
        summary.setImportedCount(items.size());
        return summary;
    }

    @Override
    public AutoDeliveryInventorySummaryRespDTO getInventorySummary(Long accountId, String xyGoodsId) {
        AutoDeliveryInventorySummaryRespDTO summary = new AutoDeliveryInventorySummaryRespDTO();
        summary.setXianyuAccountId(accountId);
        summary.setXyGoodsId(xyGoodsId);
        summary.setTotalCount(itemMapper.countAll(accountId, xyGoodsId));
        summary.setPendingCount(itemMapper.countByState(accountId, xyGoodsId, 0));
        summary.setUsedCount(itemMapper.countByState(accountId, xyGoodsId, 1));
        summary.setReservedCount(itemMapper.countByState(accountId, xyGoodsId, 2));
        summary.setImportedCount(0);
        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public XianyuGoodsAutoDeliveryItem reserveNextItem(Long accountId,
                                                       Long xianyuGoodsId,
                                                       String xyGoodsId,
                                                       Long recordId,
                                                       String orderId,
                                                       String buyerUserId,
                                                       String buyerUserName) {
        for (int i = 0; i < 5; i++) {
            XianyuGoodsAutoDeliveryItem item = itemMapper.selectNextPendingItem(accountId, xyGoodsId);
            if (item == null) {
                return null;
            }

            int updated = itemMapper.reserveItem(item.getId(), orderId, recordId, buyerUserId, buyerUserName);
            if (updated > 0) {
                item.setState(2);
                item.setXianyuGoodsId(xianyuGoodsId);
                item.setOrderId(orderId);
                item.setRecordId(recordId);
                item.setBuyerUserId(buyerUserId);
                item.setBuyerUserName(buyerUserName);
                return item;
            }
        }

        log.warn("预占自动发货库存失败: accountId={}, xyGoodsId={}, recordId={}", accountId, xyGoodsId, recordId);
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markItemUsed(Long itemId) {
        if (itemId == null) {
            return;
        }
        itemMapper.markItemUsed(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseReservedItem(Long itemId) {
        if (itemId == null) {
            return;
        }
        itemMapper.releaseReservedItem(itemId);
    }

    private List<String> parseLines(String deliveryItemsText) {
        List<String> lines = new ArrayList<>();
        if (deliveryItemsText == null || deliveryItemsText.isBlank()) {
            return lines;
        }

        String[] rawLines = deliveryItemsText.split("\\r?\\n");
        for (String rawLine : rawLines) {
            String trimmed = rawLine == null ? "" : rawLine.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return lines;
    }
}
