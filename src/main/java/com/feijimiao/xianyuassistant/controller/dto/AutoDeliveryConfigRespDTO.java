package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自动发货配置响应 DTO
 */
@Data
public class AutoDeliveryConfigRespDTO {

    private Long id;

    private Long xianyuAccountId;

    private Long xianyuGoodsId;

    private String xyGoodsId;

    /**
     * 1-固定文本 2-逐条库存发货
     */
    private Integer type;

    private String autoDeliveryContent;

    private Integer autoConfirmShipment;

    private Integer totalItemCount;

    private Integer pendingItemCount;

    private Integer usedItemCount;

    private Integer reservedItemCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
