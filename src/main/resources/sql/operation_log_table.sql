-- 操作记录表
CREATE TABLE IF NOT EXISTS xianyu_operation_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id INTEGER NOT NULL,                    -- 账号ID
    operation_type VARCHAR(50) NOT NULL,                   -- 操作类型：LOGIN, LOGOUT, SEND_MESSAGE, AUTO_DELIVERY, AUTO_REPLY, CONFIRM_SHIPMENT, TOKEN_REFRESH, WEBSOCKET_CONNECT, WEBSOCKET_DISCONNECT 等
    operation_module VARCHAR(50),                          -- 操作模块：ACCOUNT, MESSAGE, ORDER, GOODS, SYSTEM 等
    operation_desc VARCHAR(500),                           -- 操作描述
    operation_status INTEGER DEFAULT 1,                    -- 操作状态：1-成功，0-失败，2-部分成功
    target_type VARCHAR(50),                               -- 目标类型：USER, GOODS, ORDER, MESSAGE 等
    target_id VARCHAR(200),                                -- 目标ID（如商品ID、订单ID、用户ID等）
    request_params TEXT,                                   -- 请求参数（JSON格式）
    response_result TEXT,                                  -- 响应结果（JSON格式）
    error_message TEXT,                                    -- 错误信息
    ip_address VARCHAR(50),                                -- IP地址
    user_agent VARCHAR(500),                               -- 浏览器UA
    duration_ms INTEGER,                                   -- 操作耗时（毫秒）
    create_time INTEGER NOT NULL,                          -- 创建时间（时间戳，毫秒）
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_operation_log_account_id ON xianyu_operation_log(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_operation_log_type ON xianyu_operation_log(operation_type);
CREATE INDEX IF NOT EXISTS idx_operation_log_module ON xianyu_operation_log(operation_module);
CREATE INDEX IF NOT EXISTS idx_operation_log_status ON xianyu_operation_log(operation_status);
CREATE INDEX IF NOT EXISTS idx_operation_log_create_time ON xianyu_operation_log(create_time);
CREATE INDEX IF NOT EXISTS idx_operation_log_target ON xianyu_operation_log(target_type, target_id);

-- 操作类型说明
-- LOGIN: 扫码登录
-- LOGOUT: 退出登录
-- WEBSOCKET_CONNECT: WebSocket连接
-- WEBSOCKET_DISCONNECT: WebSocket断开
-- SEND_MESSAGE: 发送消息
-- RECEIVE_MESSAGE: 接收消息
-- AUTO_DELIVERY: 自动发货
-- AUTO_REPLY: 自动回复
-- CONFIRM_SHIPMENT: 确认收货
-- TOKEN_REFRESH: Token刷新
-- COOKIE_UPDATE: Cookie更新
-- GOODS_SYNC: 商品同步
-- MESSAGE_SYNC: 消息同步
