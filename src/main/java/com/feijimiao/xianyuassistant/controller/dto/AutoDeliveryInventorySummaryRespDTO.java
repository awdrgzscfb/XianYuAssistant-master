package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

/**
 * 自动发货库存汇总响应
 */
@Data
public class AutoDeliveryInventorySummaryRespDTO {

    private Long xianyuAccountId;

    private String xyGoodsId;

    private Integer totalCount;

    private Integer pendingCount;

    private Integer usedCount;

    private Integer reservedCount;

    private Integer importedCount;
}
