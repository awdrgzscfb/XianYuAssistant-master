package com.feijimiao.xianyuassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 自动发货库存明细
 */
@Data
@TableName("xianyu_goods_auto_delivery_item")
public class XianyuGoodsAutoDeliveryItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long xianyuAccountId;

    private Long xianyuGoodsId;

    private String xyGoodsId;

    private String deliveryContent;

    /**
     * 0-待使用 1-已使用 2-预占中
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
