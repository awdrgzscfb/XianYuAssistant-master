package com.feijimiao.xianyuassistant.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 自动发货配置请求 DTO
 */
@Data
public class AutoDeliveryConfigReqDTO {

    @NotNull(message = "闲鱼账号ID不能为空")
    private Long xianyuAccountId;

    private Long xianyuGoodsId;

    @NotNull(message = "闲鱼商品ID不能为空")
    private String xyGoodsId;

    /**
     * 1-固定文本 2-逐条库存发货
     */
    private Integer type = 1;

    /**
     * 固定文本发货内容
     */
    private String autoDeliveryContent;

    /**
     * 自动确认发货开关
     */
    private Integer autoConfirmShipment;
}
