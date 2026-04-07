package com.feijimiao.xianyuassistant.entity;

import lombok.Data;

/**
 * 商品自动发货记录实体类
 */
@Data
public class XianyuGoodsAutoDeliveryRecord {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 闲鱼账号ID
     */
    private Long xianyuAccountId;
    
    /**
     * 本地闲鱼商品ID
     */
    private Long xianyuGoodsId;
    
    /**
     * 闲鱼的商品ID
     */
    private String xyGoodsId;
    
    /**
     * 消息pnmid，用于防止重复发货
     */
    private String pnmId;
    
    /**
     * 买家用户ID
     */
    private String buyerUserId;
    
    /**
     * 买家用户名称
     */
    private String buyerUserName;
    
    /**
     * 发货消息内容
     */
    private String content;
    
    /**
     * 状态是否成功1-成功，0-失败
     */
    private Integer state;
    
    /**
     * 订单ID
     */
    private String orderId;
    
    /**
     * 确认发货状态：0-未确认发货，1-已确认发货
     */
    private Integer orderState;
    
    /**
     * 创建时间
     */
    private String createTime;
    
    /**
     * 商品标题（关联查询字段）
     */
    private String goodsTitle;
}
