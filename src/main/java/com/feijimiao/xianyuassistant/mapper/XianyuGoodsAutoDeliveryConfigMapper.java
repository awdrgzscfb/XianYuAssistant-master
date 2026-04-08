package com.feijimiao.xianyuassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryConfig;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品自动发货配置 Mapper
 */
@Mapper
public interface XianyuGoodsAutoDeliveryConfigMapper extends BaseMapper<XianyuGoodsAutoDeliveryConfig> {

    @Select("SELECT id, xianyu_account_id, xianyu_goods_id, xy_goods_id, type, auto_delivery_content, auto_confirm_shipment, " +
            "strftime('%Y-%m-%d %H:%M:%S', create_time) as create_time, " +
            "strftime('%Y-%m-%d %H:%M:%S', update_time) as update_time " +
            "FROM xianyu_goods_auto_delivery_config " +
            "WHERE xianyu_account_id = #{xianyuAccountId} AND xy_goods_id = #{xyGoodsId} " +
            "LIMIT 1")
    XianyuGoodsAutoDeliveryConfig findByAccountIdAndGoodsId(@Param("xianyuAccountId") Long xianyuAccountId,
                                                            @Param("xyGoodsId") String xyGoodsId);

    @Select("SELECT id, xianyu_account_id, xianyu_goods_id, xy_goods_id, type, auto_delivery_content, auto_confirm_shipment, " +
            "strftime('%Y-%m-%d %H:%M:%S', create_time) as create_time, " +
            "strftime('%Y-%m-%d %H:%M:%S', update_time) as update_time " +
            "FROM xianyu_goods_auto_delivery_config " +
            "WHERE xianyu_account_id = #{xianyuAccountId} " +
            "ORDER BY create_time DESC")
    List<XianyuGoodsAutoDeliveryConfig> findByAccountId(@Param("xianyuAccountId") Long xianyuAccountId);

    @Delete("DELETE FROM xianyu_goods_auto_delivery_config WHERE xianyu_account_id = #{xianyuAccountId}")
    int deleteByAccountId(@Param("xianyuAccountId") Long xianyuAccountId);
}
