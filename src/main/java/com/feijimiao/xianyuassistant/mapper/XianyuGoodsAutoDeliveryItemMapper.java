package com.feijimiao.xianyuassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 自动发货库存明细 Mapper
 */
@Mapper
public interface XianyuGoodsAutoDeliveryItemMapper extends BaseMapper<XianyuGoodsAutoDeliveryItem> {

    @Insert({
            "<script>",
            "INSERT INTO xianyu_goods_auto_delivery_item (",
            "xianyu_account_id, xianyu_goods_id, xy_goods_id, delivery_content, state",
            ") VALUES ",
            "<foreach collection='items' item='item' separator=','>",
            "(",
            "#{item.xianyuAccountId}, #{item.xianyuGoodsId}, #{item.xyGoodsId}, #{item.deliveryContent}, #{item.state}",
            ")",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("items") List<XianyuGoodsAutoDeliveryItem> items);

    @Select("SELECT COUNT(*) FROM xianyu_goods_auto_delivery_item WHERE xianyu_account_id = #{accountId} AND xy_goods_id = #{xyGoodsId}")
    int countAll(@Param("accountId") Long accountId, @Param("xyGoodsId") String xyGoodsId);

    @Select("SELECT COUNT(*) FROM xianyu_goods_auto_delivery_item WHERE xianyu_account_id = #{accountId} AND xy_goods_id = #{xyGoodsId} AND state = #{state}")
    int countByState(@Param("accountId") Long accountId, @Param("xyGoodsId") String xyGoodsId, @Param("state") Integer state);

    @Delete("DELETE FROM xianyu_goods_auto_delivery_item WHERE xianyu_account_id = #{accountId} AND xy_goods_id = #{xyGoodsId} AND state = 0")
    int deletePendingItems(@Param("accountId") Long accountId, @Param("xyGoodsId") String xyGoodsId);

    @Select("SELECT * FROM xianyu_goods_auto_delivery_item WHERE xianyu_account_id = #{accountId} AND xy_goods_id = #{xyGoodsId} AND state = 0 ORDER BY id ASC LIMIT 1")
    XianyuGoodsAutoDeliveryItem selectNextPendingItem(@Param("accountId") Long accountId, @Param("xyGoodsId") String xyGoodsId);

    @Update("UPDATE xianyu_goods_auto_delivery_item " +
            "SET state = 2, order_id = #{orderId}, record_id = #{recordId}, buyer_user_id = #{buyerUserId}, buyer_user_name = #{buyerUserName}, reserved_time = datetime('now') " +
            "WHERE id = #{id} AND state = 0")
    int reserveItem(@Param("id") Long id,
                    @Param("orderId") String orderId,
                    @Param("recordId") Long recordId,
                    @Param("buyerUserId") String buyerUserId,
                    @Param("buyerUserName") String buyerUserName);

    @Update("UPDATE xianyu_goods_auto_delivery_item SET state = 1, used_time = datetime('now') WHERE id = #{id} AND state = 2")
    int markItemUsed(@Param("id") Long id);

    @Update("UPDATE xianyu_goods_auto_delivery_item " +
            "SET state = 0, order_id = NULL, record_id = NULL, buyer_user_id = NULL, buyer_user_name = NULL, reserved_time = NULL " +
            "WHERE id = #{id} AND state = 2")
    int releaseReservedItem(@Param("id") Long id);
}
