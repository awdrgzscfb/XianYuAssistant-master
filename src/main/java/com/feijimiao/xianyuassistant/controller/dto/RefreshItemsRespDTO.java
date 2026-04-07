package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 刷新商品响应DTO
 */
@Data
public class RefreshItemsRespDTO {
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 刷新的商品总数
     */
    private Integer totalCount;
    
    /**
     * 成功更新的商品数量
     */
    private Integer successCount;
    
    /**
     * 更新成功的商品ID列表
     */
    private List<String> updatedItemIds;
    
    /**
     * 错误信息
     */
    private String message;
}
