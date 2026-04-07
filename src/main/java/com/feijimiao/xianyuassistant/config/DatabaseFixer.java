package com.feijimiao.xianyuassistant.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 数据库修复工具类
 * 修复时间字段为null的问题
 */
@Slf4j
@Component
public class DatabaseFixer implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("开始检查并修复数据库中的时间字段...");
        fixAutoDeliveryConfigTimeFields();
        log.info("数据库修复完成");
    }

    /**
     * 修复自动发货配置表中的时间字段
     */
    private void fixAutoDeliveryConfigTimeFields() {
        try {
            // 检查是否有时间字段为null的记录
            String checkSql = "SELECT id FROM xianyu_goods_auto_delivery_config WHERE create_time IS NULL OR update_time IS NULL LIMIT 1";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(checkSql);
            
            if (!results.isEmpty()) {
                log.info("发现自动发货配置表中存在时间字段为null的记录，开始修复...");
                
                // 修复创建时间字段
                String fixCreateTimeSql = "UPDATE xianyu_goods_auto_delivery_config SET create_time = datetime('now') WHERE create_time IS NULL";
                int createTimeFixed = jdbcTemplate.update(fixCreateTimeSql);
                log.info("修复了 {} 条记录的创建时间字段", createTimeFixed);
                
                // 修复更新时间字段
                String fixUpdateTimeSql = "UPDATE xianyu_goods_auto_delivery_config SET update_time = datetime('now') WHERE update_time IS NULL";
                int updateTimeFixed = jdbcTemplate.update(fixUpdateTimeSql);
                log.info("修复了 {} 条记录的更新时间字段", updateTimeFixed);
            } else {
                log.info("自动发货配置表中的时间字段均正常");
            }
        } catch (Exception e) {
            log.error("修复自动发货配置表时间字段失败", e);
        }
    }
}