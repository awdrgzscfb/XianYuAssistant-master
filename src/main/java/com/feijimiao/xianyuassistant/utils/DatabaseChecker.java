package com.feijimiao.xianyuassistant.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * 数据库检查工具类
 */
@Slf4j
@Component
public class DatabaseChecker {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void checkAutoDeliveryConfigData() {
        try {
            // 检查自动发货配置表中的数据
            String sql = "SELECT id, xianyu_account_id, xy_goods_id, create_time, update_time FROM xianyu_goods_auto_delivery_config LIMIT 5";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            
            log.info("自动发货配置表数据检查:");
            for (Map<String, Object> row : results) {
                log.info("  ID: {}, 账号ID: {}, 商品ID: {}, 创建时间: {}, 更新时间: {}", 
                        row.get("id"), 
                        row.get("xianyu_account_id"), 
                        row.get("xy_goods_id"), 
                        row.get("create_time"), 
                        row.get("update_time"));
            }
        } catch (Exception e) {
            log.error("检查自动发货配置表数据失败", e);
        }
    }
}