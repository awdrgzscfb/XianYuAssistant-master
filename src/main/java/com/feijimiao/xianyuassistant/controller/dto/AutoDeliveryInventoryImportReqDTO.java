package com.feijimiao.xianyuassistant.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 自动发货库存导入请求
 */
@Data
public class AutoDeliveryInventoryImportReqDTO {

    @NotNull(message = "闲鱼账号ID不能为空")
    private Long xianyuAccountId;

    private Long xianyuGoodsId;

    @NotBlank(message = "闲鱼商品ID不能为空")
    private String xyGoodsId;

    /**
     * 多行导入文本，一行一条
     */
    @NotBlank(message = "导入内容不能为空")
    private String deliveryItemsText;

    /**
     * 是否覆盖未使用库存
     */
    private Boolean replacePendingItems = Boolean.FALSE;
}
