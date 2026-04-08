package com.feijimiao.xianyuassistant.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * 数据库初始化监听器
 * 自动检测并创建缺失的表和字段
 */
@Slf4j
@Component
public class DatabaseInitListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private DataSource dataSource;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 打印数据库文件路径
        try (Connection conn = dataSource.getConnection()) {
            String url = conn.getMetaData().getURL();
            String dbPath = url.replace("jdbc:sqlite:", "");
            File dbFile = new File(dbPath);
            log.info("数据库文件路径: {}", dbFile.getCanonicalPath());
        } catch (Exception e) {
            log.warn("获取数据库文件路径失败: {}", e.getMessage());
        }
        
        log.info("=".repeat(60));
        log.info("开始数据库自动迁移...");
        log.info("=".repeat(60));
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // 1. 检查并创建缺失的表
            checkAndCreateTables(stmt);
            
            // 2. 检查并添加缺失的字段
            checkAndAddColumns(stmt);
            
            // 3. 检查并创建缺失的索引
            checkAndCreateIndexes(stmt);
            
            // 4. 检查并创建缺失的触发器
            checkAndCreateTriggers(stmt);
            
            log.info("=".repeat(60));
            log.info("数据库迁移完成，开始验证...");
            log.info("=".repeat(60));
            
            // 验证数据库状态
            verifyDatabase(stmt);
            
            log.info("=".repeat(60));
            log.info("✅ 数据库验证完成，系统就绪！");
            log.info("=".repeat(60));
            
        } catch (Exception e) {
            log.error("数据库初始化失败", e);
        }
    }
    
    /**
     * 检查并创建缺失的表
     */
    private void checkAndCreateTables(Statement stmt) throws Exception {
        log.info("🔍 检查数据库表...");
        
        // 获取现有表列表
        Set<String> existingTables = new HashSet<>();
        ResultSet tables = stmt.executeQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"
        );
        while (tables.next()) {
            existingTables.add(tables.getString("name"));
        }
        tables.close();
        
        // 定义需要的表及其创建SQL
        Map<String, String> requiredTables = new LinkedHashMap<>();
        
        // 闲鱼账号表
        requiredTables.put("xianyu_account", 
            "CREATE TABLE xianyu_account (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "account_note VARCHAR(100), " +
            "unb VARCHAR(100), " +
            "status TINYINT DEFAULT 1, " +
            "created_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "updated_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ")");
        
        // 闲鱼Cookie表
        requiredTables.put("xianyu_cookie",
            "CREATE TABLE xianyu_cookie (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "xianyu_account_id BIGINT NOT NULL, " +
            "cookie_text TEXT, " +
            "m_h5_tk VARCHAR(500), " +
            "cookie_status TINYINT DEFAULT 1, " +
            "expire_time DATETIME, " +
            "websocket_token TEXT, " +
            "token_expire_time INTEGER, " +
            "created_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "updated_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)" +
            ")");
        
        // 闲鱼商品信息表
        requiredTables.put("xianyu_goods",
            "CREATE TABLE xianyu_goods (" +
            "id BIGINT PRIMARY KEY, " +
            "xy_good_id VARCHAR(100) NOT NULL, " +
            "xianyu_account_id BIGINT, " +
            "title VARCHAR(500), " +
            "cover_pic TEXT, " +
            "info_pic TEXT, " +
            "detail_info TEXT, " +
            "detail_url TEXT, " +
            "sold_price VARCHAR(50), " +
            "status TINYINT DEFAULT 0, " +
            "created_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "updated_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)" +
            ")");
        
        // 闲鱼聊天消息表
        requiredTables.put("xianyu_chat_message",
            "CREATE TABLE xianyu_chat_message (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "xianyu_account_id BIGINT NOT NULL, " +
            "lwp VARCHAR(50), " +
            "pnm_id VARCHAR(100) NOT NULL, " +
            "s_id VARCHAR(100), " +
            "content_type INTEGER, " +
            "msg_content TEXT, " +
            "sender_user_name VARCHAR(200), " +
            "sender_user_id VARCHAR(100), " +
            "sender_app_v VARCHAR(50), " +
            "sender_os_type VARCHAR(20), " +
            "reminder_url TEXT, " +
            "xy_goods_id VARCHAR(100), " +
            "complete_msg TEXT NOT NULL, " +
            "message_time BIGINT, " +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)" +
            ")");
        
        // 商品配置表
        requiredTables.put("xianyu_goods_config",
            "CREATE TABLE xianyu_goods_config (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "xianyu_account_id BIGINT NOT NULL, " +
            "xianyu_goods_id BIGINT, " +
            "xy_goods_id VARCHAR(100) NOT NULL, " +
            "xianyu_auto_delivery_on TINYINT DEFAULT 0, " +
            "xianyu_auto_reply_on TINYINT DEFAULT 0, " +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)" +
            ")");
        
        // 商品自动发货配置表
        requiredTables.put("xianyu_goods_auto_delivery_config",
            "CREATE TABLE xianyu_goods_auto_delivery_config (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "xianyu_account_id BIGINT NOT NULL, " +
            "xianyu_goods_id BIGINT, " +
            "xy_goods_id VARCHAR(100) NOT NULL, " +
            "type TINYINT DEFAULT 1, " +
            "auto_delivery_content TEXT, " +
            "auto_confirm_shipment TINYINT DEFAULT 0, " +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)" +
            ")");

        // 商品自动发货库存表
        requiredTables.put("xianyu_goods_auto_delivery_item",
            "CREATE TABLE xianyu_goods_auto_delivery_item (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "xianyu_account_id BIGINT NOT NULL, " +
            "xianyu_goods_id BIGINT, " +
            "xy_goods_id VARCHAR(100) NOT NULL, " +
            "delivery_content TEXT NOT NULL, " +
            "state TINYINT DEFAULT 0, " +
            "order_id VARCHAR(100), " +
            "record_id BIGINT, " +
            "buyer_user_id VARCHAR(100), " +
            "buyer_user_name VARCHAR(100), " +
            "reserved_time DATETIME, " +
            "used_time DATETIME, " +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)" +
            ")");
        
        // 商品自动发货记录表
        requiredTables.put("xianyu_goods_auto_delivery_record",
            "CREATE TABLE xianyu_goods_auto_delivery_record (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "xianyu_account_id BIGINT NOT NULL, " +
            "xianyu_goods_id BIGINT, " +
            "xy_goods_id VARCHAR(100) NOT NULL, " +
            "pnm_id VARCHAR(100) NOT NULL, " +
            "buyer_user_id VARCHAR(100), " +
            "buyer_user_name VARCHAR(100), " +
            "content TEXT, " +
            "state TINYINT DEFAULT 0, " +
            "order_id VARCHAR(100), " +
            "order_state TINYINT DEFAULT 0, " +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)" +
            ")");
        
        // 商品自动回复配置表
        requiredTables.put("xianyu_goods_auto_reply_config",
            "CREATE TABLE xianyu_goods_auto_reply_config (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "xianyu_account_id BIGINT NOT NULL, " +
            "xianyu_goods_id BIGINT, " +
            "xy_goods_id VARCHAR(100) NOT NULL, " +
            "keyword TEXT, " +
            "reply_content TEXT, " +
            "match_type TINYINT DEFAULT 1, " +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)" +
            ")");
        
        // 商品自动回复记录表
        requiredTables.put("xianyu_goods_auto_reply_record",
            "CREATE TABLE xianyu_goods_auto_reply_record (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "xianyu_account_id BIGINT NOT NULL, " +
            "xianyu_goods_id BIGINT, " +
            "xy_goods_id VARCHAR(100) NOT NULL, " +
            "buyer_message TEXT, " +
            "reply_content TEXT, " +
            "matched_keyword VARCHAR(200), " +
            "state TINYINT DEFAULT 0, " +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)" +
            ")");
        
        // 操作记录表
        requiredTables.put("xianyu_operation_log",
            "CREATE TABLE xianyu_operation_log (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "xianyu_account_id INTEGER NOT NULL, " +
            "operation_type VARCHAR(50) NOT NULL, " +
            "operation_module VARCHAR(50), " +
            "operation_desc VARCHAR(500), " +
            "operation_status INTEGER DEFAULT 1, " +
            "target_type VARCHAR(50), " +
            "target_id VARCHAR(200), " +
            "request_params TEXT, " +
            "response_result TEXT, " +
            "error_message TEXT, " +
            "ip_address VARCHAR(50), " +
            "user_agent VARCHAR(500), " +
            "duration_ms INTEGER, " +
            "create_time INTEGER NOT NULL, " +
            "FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)" +
            ")");
        
        // 检查并创建缺失的表
        int createdCount = 0;
        for (Map.Entry<String, String> entry : requiredTables.entrySet()) {
            String tableName = entry.getKey();
            String createSql = entry.getValue();
            
            if (!existingTables.contains(tableName)) {
                log.info("  ➕ 创建表: {}", tableName);
                stmt.execute(createSql);
                createdCount++;
            } else {
                log.info("  ✓ 表已存在: {}", tableName);
            }
        }
        
        if (createdCount > 0) {
            log.info("✅ 创建了 {} 个新表", createdCount);
        }
    }
    
    /**
     * 检查并添加缺失的字段
     */
    private void checkAndAddColumns(Statement stmt) throws Exception {
        log.info("🔍 检查表字段...");
        
        // 定义需要检查的表和字段
        Map<String, List<ColumnDef>> tableColumns = new LinkedHashMap<>();
        
        // xianyu_cookie 表需要的字段
        List<ColumnDef> cookieColumns = new ArrayList<>();
        cookieColumns.add(new ColumnDef("m_h5_tk", "VARCHAR(500)", "ALTER TABLE xianyu_cookie ADD COLUMN m_h5_tk VARCHAR(500)"));
        cookieColumns.add(new ColumnDef("websocket_token", "TEXT", "ALTER TABLE xianyu_cookie ADD COLUMN websocket_token TEXT"));
        cookieColumns.add(new ColumnDef("token_expire_time", "INTEGER", "ALTER TABLE xianyu_cookie ADD COLUMN token_expire_time INTEGER"));
        tableColumns.put("xianyu_cookie", cookieColumns);
        
        // xianyu_goods 表需要的字段
        List<ColumnDef> goodsColumns = new ArrayList<>();
        goodsColumns.add(new ColumnDef("detail_url", "TEXT", "ALTER TABLE xianyu_goods ADD COLUMN detail_url TEXT"));
        goodsColumns.add(new ColumnDef("xianyu_account_id", "BIGINT", "ALTER TABLE xianyu_goods ADD COLUMN xianyu_account_id BIGINT"));
        tableColumns.put("xianyu_goods", goodsColumns);
        
        // xianyu_chat_message 表需要的字段
        List<ColumnDef> chatMessageColumns = new ArrayList<>();
        chatMessageColumns.add(new ColumnDef("xy_goods_id", "VARCHAR(100)", "ALTER TABLE xianyu_chat_message ADD COLUMN xy_goods_id VARCHAR(100)"));
        tableColumns.put("xianyu_chat_message", chatMessageColumns);
        
        // xianyu_goods_auto_delivery_record 表需要的字段
        List<ColumnDef> deliveryRecordColumns = new ArrayList<>();
        deliveryRecordColumns.add(new ColumnDef("content", "TEXT", "ALTER TABLE xianyu_goods_auto_delivery_record ADD COLUMN content TEXT"));
        deliveryRecordColumns.add(new ColumnDef("buyer_user_name", "VARCHAR(100)", "ALTER TABLE xianyu_goods_auto_delivery_record ADD COLUMN buyer_user_name VARCHAR(100)"));
        deliveryRecordColumns.add(new ColumnDef("pnm_id", "VARCHAR(100)", "ALTER TABLE xianyu_goods_auto_delivery_record ADD COLUMN pnm_id VARCHAR(100)"));
        deliveryRecordColumns.add(new ColumnDef("order_id", "VARCHAR(100)", "ALTER TABLE xianyu_goods_auto_delivery_record ADD COLUMN order_id VARCHAR(100)"));
        deliveryRecordColumns.add(new ColumnDef("order_state", "TINYINT", "ALTER TABLE xianyu_goods_auto_delivery_record ADD COLUMN order_state TINYINT DEFAULT 0"));
        tableColumns.put("xianyu_goods_auto_delivery_record", deliveryRecordColumns);
        
        // xianyu_goods_auto_delivery_config 表需要的字段
        List<ColumnDef> deliveryConfigColumns = new ArrayList<>();
        deliveryConfigColumns.add(new ColumnDef("auto_confirm_shipment", "TINYINT", "ALTER TABLE xianyu_goods_auto_delivery_config ADD COLUMN auto_confirm_shipment TINYINT DEFAULT 0"));
        tableColumns.put("xianyu_goods_auto_delivery_config", deliveryConfigColumns);
        
        int addedCount = 0;
        for (Map.Entry<String, List<ColumnDef>> entry : tableColumns.entrySet()) {
            String tableName = entry.getKey();
            List<ColumnDef> columns = entry.getValue();
            
            // 检查表是否存在
            if (!tableExists(stmt, tableName)) {
                continue;
            }
            
            // 获取表的现有字段
            Set<String> existingColumns = getTableColumns(stmt, tableName);
            
            // 检查并添加缺失的字段
            for (ColumnDef column : columns) {
                if (!existingColumns.contains(column.name.toLowerCase())) {
                    log.info("  ➕ 添加字段: {}.{}", tableName, column.name);
                    stmt.execute(column.alterSql);
                    addedCount++;
                    
                    // 特殊处理：如果是 xianyu_goods_auto_delivery_record 表的 pnm_id 字段
                    if ("xianyu_goods_auto_delivery_record".equals(tableName) && "pnm_id".equals(column.name)) {
                        log.info("  🔄 为现有记录设置 pnm_id 默认值...");
                        stmt.execute("UPDATE xianyu_goods_auto_delivery_record SET pnm_id = 'LEGACY_' || id WHERE pnm_id IS NULL");
                    }
                } else {
                    log.debug("  ✓ 字段已存在: {}.{}", tableName, column.name);
                }
            }
        }
        
        if (addedCount > 0) {
            log.info("✅ 添加了 {} 个新字段", addedCount);
        } else {
            log.info("✓ 所有字段都已存在");
        }
    }
    
    /**
     * 检查并创建缺失的索引
     */
    private void checkAndCreateIndexes(Statement stmt) throws Exception {
        log.info("🔍 检查数据库索引...");
        
        // 获取现有索引
        Set<String> existingIndexes = new HashSet<>();
        ResultSet indexes = stmt.executeQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND name NOT LIKE 'sqlite_%'"
        );
        while (indexes.next()) {
            existingIndexes.add(indexes.getString("name"));
        }
        indexes.close();
        
        // 定义需要的索引
        Map<String, String> requiredIndexes = new LinkedHashMap<>();
        requiredIndexes.put("idx_account_unb", 
            "CREATE INDEX IF NOT EXISTS idx_account_unb ON xianyu_account(unb)");
        requiredIndexes.put("idx_cookie_account_id",
            "CREATE INDEX IF NOT EXISTS idx_cookie_account_id ON xianyu_cookie(xianyu_account_id)");
        requiredIndexes.put("idx_cookie_status",
            "CREATE INDEX IF NOT EXISTS idx_cookie_status ON xianyu_cookie(cookie_status)");
        requiredIndexes.put("idx_token_expire_time",
            "CREATE INDEX IF NOT EXISTS idx_token_expire_time ON xianyu_cookie(token_expire_time)");
        requiredIndexes.put("idx_goods_xy_good_id",
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_goods_xy_good_id ON xianyu_goods(xy_good_id)");
        requiredIndexes.put("idx_goods_status",
            "CREATE INDEX IF NOT EXISTS idx_goods_status ON xianyu_goods(status)");
        requiredIndexes.put("idx_goods_account_id",
            "CREATE INDEX IF NOT EXISTS idx_goods_account_id ON xianyu_goods(xianyu_account_id)");
        
        // 聊天消息表索引
        requiredIndexes.put("idx_chat_message_account_id",
            "CREATE INDEX IF NOT EXISTS idx_chat_message_account_id ON xianyu_chat_message(xianyu_account_id)");
        requiredIndexes.put("idx_chat_message_pnm_id",
            "CREATE INDEX IF NOT EXISTS idx_chat_message_pnm_id ON xianyu_chat_message(pnm_id)");
        requiredIndexes.put("idx_chat_message_s_id",
            "CREATE INDEX IF NOT EXISTS idx_chat_message_s_id ON xianyu_chat_message(s_id)");
        requiredIndexes.put("idx_chat_message_sender_user_id",
            "CREATE INDEX IF NOT EXISTS idx_chat_message_sender_user_id ON xianyu_chat_message(sender_user_id)");
        requiredIndexes.put("idx_chat_message_content_type",
            "CREATE INDEX IF NOT EXISTS idx_chat_message_content_type ON xianyu_chat_message(content_type)");
        requiredIndexes.put("idx_chat_message_time",
            "CREATE INDEX IF NOT EXISTS idx_chat_message_time ON xianyu_chat_message(message_time)");
        requiredIndexes.put("idx_chat_message_goods_id",
            "CREATE INDEX IF NOT EXISTS idx_chat_message_goods_id ON xianyu_chat_message(xy_goods_id)");
        requiredIndexes.put("idx_chat_message_unique",
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_message_unique ON xianyu_chat_message(xianyu_account_id, pnm_id)");
        
        // 商品配置表索引
        requiredIndexes.put("idx_goods_config_account_id",
            "CREATE INDEX IF NOT EXISTS idx_goods_config_account_id ON xianyu_goods_config(xianyu_account_id)");
        requiredIndexes.put("idx_goods_config_xy_goods_id",
            "CREATE INDEX IF NOT EXISTS idx_goods_config_xy_goods_id ON xianyu_goods_config(xy_goods_id)");
        requiredIndexes.put("idx_goods_config_unique",
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_goods_config_unique ON xianyu_goods_config(xianyu_account_id, xy_goods_id)");
        
        // 自动发货配置表索引
        requiredIndexes.put("idx_auto_delivery_config_account_id",
            "CREATE INDEX IF NOT EXISTS idx_auto_delivery_config_account_id ON xianyu_goods_auto_delivery_config(xianyu_account_id)");
        requiredIndexes.put("idx_auto_delivery_config_xy_goods_id",
            "CREATE INDEX IF NOT EXISTS idx_auto_delivery_config_xy_goods_id ON xianyu_goods_auto_delivery_config(xy_goods_id)");
        requiredIndexes.put("idx_auto_delivery_config_unique",
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_auto_delivery_config_unique ON xianyu_goods_auto_delivery_config(xianyu_account_id, xy_goods_id)");

        // 自动发货库存表索引
        requiredIndexes.put("idx_auto_delivery_item_account_id",
            "CREATE INDEX IF NOT EXISTS idx_auto_delivery_item_account_id ON xianyu_goods_auto_delivery_item(xianyu_account_id)");
        requiredIndexes.put("idx_auto_delivery_item_xy_goods_id",
            "CREATE INDEX IF NOT EXISTS idx_auto_delivery_item_xy_goods_id ON xianyu_goods_auto_delivery_item(xy_goods_id)");
        requiredIndexes.put("idx_auto_delivery_item_state",
            "CREATE INDEX IF NOT EXISTS idx_auto_delivery_item_state ON xianyu_goods_auto_delivery_item(state)");
        requiredIndexes.put("idx_auto_delivery_item_record_id",
            "CREATE INDEX IF NOT EXISTS idx_auto_delivery_item_record_id ON xianyu_goods_auto_delivery_item(record_id)");
        
        // 自动发货记录表索引
        requiredIndexes.put("idx_auto_delivery_record_account_id",
            "CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_account_id ON xianyu_goods_auto_delivery_record(xianyu_account_id)");
        requiredIndexes.put("idx_auto_delivery_record_xy_goods_id",
            "CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_xy_goods_id ON xianyu_goods_auto_delivery_record(xy_goods_id)");
        requiredIndexes.put("idx_auto_delivery_record_state",
            "CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_state ON xianyu_goods_auto_delivery_record(state)");
        requiredIndexes.put("idx_auto_delivery_record_create_time",
            "CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_create_time ON xianyu_goods_auto_delivery_record(create_time)");
        requiredIndexes.put("idx_auto_delivery_record_pnm_id",
            "CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_pnm_id ON xianyu_goods_auto_delivery_record(pnm_id)");
        requiredIndexes.put("idx_auto_delivery_record_order_id",
            "CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_order_id ON xianyu_goods_auto_delivery_record(order_id)");
        requiredIndexes.put("idx_auto_delivery_record_order_state",
            "CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_order_state ON xianyu_goods_auto_delivery_record(order_state)");
        requiredIndexes.put("idx_auto_delivery_record_unique",
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_auto_delivery_record_unique ON xianyu_goods_auto_delivery_record(xianyu_account_id, pnm_id)");
        
        // 自动回复配置表索引
        requiredIndexes.put("idx_auto_reply_config_account_id",
            "CREATE INDEX IF NOT EXISTS idx_auto_reply_config_account_id ON xianyu_goods_auto_reply_config(xianyu_account_id)");
        requiredIndexes.put("idx_auto_reply_config_xy_goods_id",
            "CREATE INDEX IF NOT EXISTS idx_auto_reply_config_xy_goods_id ON xianyu_goods_auto_reply_config(xy_goods_id)");
        
        // 自动回复记录表索引
        requiredIndexes.put("idx_auto_reply_record_account_id",
            "CREATE INDEX IF NOT EXISTS idx_auto_reply_record_account_id ON xianyu_goods_auto_reply_record(xianyu_account_id)");
        requiredIndexes.put("idx_auto_reply_record_xy_goods_id",
            "CREATE INDEX IF NOT EXISTS idx_auto_reply_record_xy_goods_id ON xianyu_goods_auto_reply_record(xy_goods_id)");
        requiredIndexes.put("idx_auto_reply_record_state",
            "CREATE INDEX IF NOT EXISTS idx_auto_reply_record_state ON xianyu_goods_auto_reply_record(state)");
        requiredIndexes.put("idx_auto_reply_record_create_time",
            "CREATE INDEX IF NOT EXISTS idx_auto_reply_record_create_time ON xianyu_goods_auto_reply_record(create_time)");
        
        // 操作记录表索引
        requiredIndexes.put("idx_operation_log_account_id",
            "CREATE INDEX IF NOT EXISTS idx_operation_log_account_id ON xianyu_operation_log(xianyu_account_id)");
        requiredIndexes.put("idx_operation_log_type",
            "CREATE INDEX IF NOT EXISTS idx_operation_log_type ON xianyu_operation_log(operation_type)");
        requiredIndexes.put("idx_operation_log_module",
            "CREATE INDEX IF NOT EXISTS idx_operation_log_module ON xianyu_operation_log(operation_module)");
        requiredIndexes.put("idx_operation_log_status",
            "CREATE INDEX IF NOT EXISTS idx_operation_log_status ON xianyu_operation_log(operation_status)");
        requiredIndexes.put("idx_operation_log_create_time",
            "CREATE INDEX IF NOT EXISTS idx_operation_log_create_time ON xianyu_operation_log(create_time)");
        requiredIndexes.put("idx_operation_log_target",
            "CREATE INDEX IF NOT EXISTS idx_operation_log_target ON xianyu_operation_log(target_type, target_id)");
        
        int createdCount = 0;
        for (Map.Entry<String, String> entry : requiredIndexes.entrySet()) {
            String indexName = entry.getKey();
            String createSql = entry.getValue();
            
            if (!existingIndexes.contains(indexName)) {
                log.info("  ➕ 创建索引: {}", indexName);
                stmt.execute(createSql);
                createdCount++;
            } else {
                log.debug("  ✓ 索引已存在: {}", indexName);
            }
        }
        
        if (createdCount > 0) {
            log.info("✅ 创建了 {} 个新索引", createdCount);
        } else {
            log.info("✓ 所有索引都已存在");
        }
    }
    
    /**
     * 检查并创建缺失的触发器
     */
    private void checkAndCreateTriggers(Statement stmt) throws Exception {
        log.info("🔍 检查数据库触发器...");
        
        // 获取现有触发器
        Set<String> existingTriggers = new HashSet<>();
        ResultSet triggers = stmt.executeQuery(
            "SELECT name FROM sqlite_master WHERE type='trigger'"
        );
        while (triggers.next()) {
            existingTriggers.add(triggers.getString("name"));
        }
        triggers.close();
        
        // 定义需要的触发器
        Map<String, String> requiredTriggers = new LinkedHashMap<>();
        requiredTriggers.put("update_xianyu_account_time",
            "CREATE TRIGGER IF NOT EXISTS update_xianyu_account_time " +
            "AFTER UPDATE ON xianyu_account " +
            "BEGIN " +
            "UPDATE xianyu_account SET updated_time = CURRENT_TIMESTAMP WHERE id = NEW.id; " +
            "END");
        requiredTriggers.put("update_xianyu_cookie_time",
            "CREATE TRIGGER IF NOT EXISTS update_xianyu_cookie_time " +
            "AFTER UPDATE ON xianyu_cookie " +
            "BEGIN " +
            "UPDATE xianyu_cookie SET updated_time = CURRENT_TIMESTAMP WHERE id = NEW.id; " +
            "END");
        requiredTriggers.put("update_xianyu_goods_time",
            "CREATE TRIGGER IF NOT EXISTS update_xianyu_goods_time " +
            "AFTER UPDATE ON xianyu_goods " +
            "BEGIN " +
            "UPDATE xianyu_goods SET updated_time = CURRENT_TIMESTAMP WHERE id = NEW.id; " +
            "END");
        requiredTriggers.put("update_xianyu_chat_message_time",
            "CREATE TRIGGER IF NOT EXISTS update_xianyu_chat_message_time " +
            "AFTER UPDATE ON xianyu_chat_message " +
            "BEGIN " +
            "UPDATE xianyu_chat_message SET updated_time = CURRENT_TIMESTAMP WHERE id = NEW.id; " +
            "END");
        requiredTriggers.put("update_xianyu_goods_config_time",
            "CREATE TRIGGER IF NOT EXISTS update_xianyu_goods_config_time " +
            "AFTER UPDATE ON xianyu_goods_config " +
            "BEGIN " +
            "UPDATE xianyu_goods_config SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id; " +
            "END");
        requiredTriggers.put("update_xianyu_goods_auto_delivery_config_time",
            "CREATE TRIGGER IF NOT EXISTS update_xianyu_goods_auto_delivery_config_time " +
            "AFTER UPDATE ON xianyu_goods_auto_delivery_config " +
            "BEGIN " +
            "UPDATE xianyu_goods_auto_delivery_config SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id; " +
            "END");
        requiredTriggers.put("update_xianyu_goods_auto_reply_config_time",
            "CREATE TRIGGER IF NOT EXISTS update_xianyu_goods_auto_reply_config_time " +
            "AFTER UPDATE ON xianyu_goods_auto_reply_config " +
            "BEGIN " +
            "UPDATE xianyu_goods_auto_reply_config SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id; " +
            "END");
        
        int createdCount = 0;
        for (Map.Entry<String, String> entry : requiredTriggers.entrySet()) {
            String triggerName = entry.getKey();
            String createSql = entry.getValue();
            
            if (!existingTriggers.contains(triggerName)) {
                log.info("  ➕ 创建触发器: {}", triggerName);
                stmt.execute(createSql);
                createdCount++;
            } else {
                log.debug("  ✓ 触发器已存在: {}", triggerName);
            }
        }
        
        if (createdCount > 0) {
            log.info("✅ 创建了 {} 个新触发器", createdCount);
        } else {
            log.info("✓ 所有触发器都已存在");
        }
    }
    
    /**
     * 验证数据库状态
     */
    private void verifyDatabase(Statement stmt) throws Exception {
        // 查询表信息
        ResultSet tables = stmt.executeQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name"
        );
        
        log.info("📊 数据库表列表:");
        while (tables.next()) {
            String tableName = tables.getString("name");
            
            // 查询表的记录数
            ResultSet count = stmt.executeQuery("SELECT COUNT(*) as cnt FROM " + tableName);
            int recordCount = 0;
            if (count.next()) {
                recordCount = count.getInt("cnt");
            }
            count.close();
            
            log.info("  ✓ {} (记录数: {})", tableName, recordCount);
        }
        tables.close();
    }
    
    /**
     * 检查表是否存在
     */
    private boolean tableExists(Statement stmt, String tableName) throws Exception {
        ResultSet rs = stmt.executeQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'"
        );
        boolean exists = rs.next();
        rs.close();
        return exists;
    }
    
    /**
     * 获取表的所有字段名
     */
    private Set<String> getTableColumns(Statement stmt, String tableName) throws Exception {
        Set<String> columns = new HashSet<>();
        ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")");
        while (rs.next()) {
            columns.add(rs.getString("name").toLowerCase());
        }
        rs.close();
        return columns;
    }
    
    /**
     * 字段定义
     */
    private static class ColumnDef {
        String name;
        String type;
        String alterSql;
        
        ColumnDef(String name, String type, String alterSql) {
            this.name = name;
            this.type = type;
            this.alterSql = alterSql;
        }
    }
}
