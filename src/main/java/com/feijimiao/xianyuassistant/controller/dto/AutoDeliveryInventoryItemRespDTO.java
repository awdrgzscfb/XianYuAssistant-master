package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

@Data
public class AutoDeliveryInventoryItemRespDTO {

    private Long id;

    private Long xianyuAccountId;

    private Long xianyuGoodsId;

    private String xyGoodsId;

    private String deliveryContent;

    /**
     * 0-待发 1-已使用 2-预占中
     */
    private Integer state;

    private String orderId;

    private Long recordId;

    private String buyerUserId;

    private String buyerUserName;

    private String reservedTime;

    private String usedTime;

    private String createTime;
}
