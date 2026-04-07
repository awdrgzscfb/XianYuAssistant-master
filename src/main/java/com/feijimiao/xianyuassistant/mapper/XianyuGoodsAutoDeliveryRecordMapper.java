package com.feijimiao.xianyuassistant.mapper;

import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 商品自动发货记录Mapper
 */
@Mapper
public interface XianyuGoodsAutoDeliveryRecordMapper {
    
    /**
     * 插入记录
     */
    @Insert("INSERT INTO xianyu_goods_auto_delivery_record (xianyu_account_id, xianyu_goods_id, xy_goods_id, pnm_id, buyer_user_id, buyer_user_name, content, state, order_id) " +
            "VALUES (#{xianyuAccountId}, #{xianyuGoodsId}, #{xyGoodsId}, #{pnmId}, #{buyerUserId}, #{buyerUserName}, #{content}, #{state}, #{orderId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(XianyuGoodsAutoDeliveryRecord record);
    
    /**
     * 根据账号ID查询记录
     */
    @Select("SELECT * FROM xianyu_goods_auto_delivery_record WHERE xianyu_account_id = #{accountId} ORDER BY create_time DESC")
    List<XianyuGoodsAutoDeliveryRecord> selectByAccountId(@Param("accountId") Long accountId);
    
    /**
     * 根据账号ID删除记录
     */
    @Delete("DELETE FROM xianyu_goods_auto_delivery_record WHERE xianyu_account_id = #{accountId}")
    int deleteByAccountId(@Param("accountId") Long accountId);
    
    /**
     * 根据账号ID和商品ID查询记录（分页）
     */
    @Select("<script>" +
            "SELECT r.*, g.title as goods_title " +
            "FROM xianyu_goods_auto_delivery_record r " +
            "LEFT JOIN xianyu_goods g ON r.xy_goods_id = g.xy_good_id " +
            "WHERE r.xianyu_account_id = #{accountId} " +
            "<if test='xyGoodsId != null and xyGoodsId != \"\"'>" +
            "AND r.xy_goods_id = #{xyGoodsId} " +
            "</if>" +
            "ORDER BY r.create_time DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "xianyuAccountId", column = "xianyu_account_id"),
        @Result(property = "xianyuGoodsId", column = "xianyu_goods_id"),
        @Result(property = "xyGoodsId", column = "xy_goods_id"),
        @Result(property = "buyerUserId", column = "buyer_user_id"),
        @Result(property = "buyerUserName", column = "buyer_user_name"),
        @Result(property = "content", column = "content"),
        @Result(property = "state", column = "state"),
        @Result(property = "orderId", column = "order_id"),
        @Result(property = "orderState", column = "order_state"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "goodsTitle", column = "goods_title")
    })
    List<XianyuGoodsAutoDeliveryRecord> selectByAccountIdWithPage(
            @Param("accountId") Long accountId,
            @Param("xyGoodsId") String xyGoodsId,
            @Param("limit") int limit,
            @Param("offset") int offset);
    
    /**
     * 统计记录总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM xianyu_goods_auto_delivery_record " +
            "WHERE xianyu_account_id = #{accountId} " +
            "<if test='xyGoodsId != null and xyGoodsId != \"\"'>" +
            "AND xy_goods_id = #{xyGoodsId} " +
            "</if>" +
            "</script>")
    long countByAccountId(@Param("accountId") Long accountId, @Param("xyGoodsId") String xyGoodsId);
    
    /**
     * 更新发货记录状态
     */
    @Update("UPDATE xianyu_goods_auto_delivery_record SET state = #{state} WHERE id = #{id}")
    int updateState(@Param("id") Long id, @Param("state") Integer state);
    
    /**
     * 更新发货记录状态和内容
     */
    @Update("UPDATE xianyu_goods_auto_delivery_record SET state = #{state}, content = #{content} WHERE id = #{id}")
    int updateStateAndContent(@Param("id") Long id, @Param("state") Integer state, @Param("content") String content);
    
    /**
     * 更新确认发货状态
     */
    @Update("UPDATE xianyu_goods_auto_delivery_record SET order_state = #{orderState} WHERE xianyu_account_id = #{accountId} AND order_id = #{orderId}")
    int updateOrderState(@Param("accountId") Long accountId, @Param("orderId") String orderId, @Param("orderState") Integer orderState);
}