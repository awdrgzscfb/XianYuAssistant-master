package com.feijimiao.xianyuassistant.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AutoDeliveryInventoryItemDeleteReqDTO {

    @NotNull(message = "闲鱼账号ID不能为空")
    private Long xianyuAccountId;

    @NotBlank(message = "闲鱼商品ID不能为空")
    private String xyGoodsId;

    @NotNull(message = "库存项ID不能为空")
    private Long itemId;
}
