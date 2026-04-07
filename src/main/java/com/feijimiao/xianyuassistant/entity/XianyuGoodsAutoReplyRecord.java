package com.feijimiao.xianyuassistant.entity;

import lombok.Data;

/**
 * 商品自动回复记录实体类
 */
@Data
public class XianyuGoodsAutoReplyRecord {
    
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
     * 买家消息内容
     */
    private String buyerMessage;
    
    /**
     * 回复消息内容
     */
    private String replyContent;
    
    /**
     * 匹配的关键词
     */
    private String matchedKeyword;
    
    /**
     * 状态是否成功1-成功，0-失败
     */
    private Integer state;
    
    /**
     * 创建时间
     */
    private String createTime;
}
