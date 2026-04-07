-- 闲鱼账号表
CREATE TABLE IF NOT EXISTS xianyu_account (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_note VARCHAR(100),                    -- 闲鱼账号备注
    unb VARCHAR(100),                             -- UNB标识
    status TINYINT DEFAULT 1,                     -- 账号状态 1:正常 -1:需要手机号验证
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP   -- 更新时间
);

-- 闲鱼Cookie表
CREATE TABLE IF NOT EXISTS xianyu_cookie (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT NOT NULL,            -- 关联的闲鱼账号ID
    cookie_text TEXT,                             -- 完整的Cookie字符串
    m_h5_tk VARCHAR(500),                         -- _m_h5_tk token（用于API签名）
    cookie_status TINYINT DEFAULT 1,              -- Cookie状态 1:有效 2:过期 3:失效
    expire_time DATETIME,                         -- 过期时间
    websocket_token TEXT,                         -- WebSocket accessToken
    token_expire_time INTEGER,                    -- Token过期时间戳（毫秒）
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_account_unb ON xianyu_account(unb);
CREATE INDEX IF NOT EXISTS idx_cookie_account_id ON xianyu_cookie(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_cookie_status ON xianyu_cookie(cookie_status);
CREATE INDEX IF NOT EXISTS idx_token_expire_time ON xianyu_cookie(token_expire_time);

-- 创建更新时间触发器（SQLite不支持ON UPDATE CURRENT_TIMESTAMP，需要用触发器）
-- 注意：触发器使用特殊分隔符 $$
CREATE TRIGGER IF NOT EXISTS update_xianyu_account_time 
AFTER UPDATE ON xianyu_account
BEGIN
    UPDATE xianyu_account SET updated_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END
$$

CREATE TRIGGER IF NOT EXISTS update_xianyu_cookie_time 
AFTER UPDATE ON xianyu_cookie
BEGIN
    UPDATE xianyu_cookie SET updated_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END
$$

-- 闲鱼商品信息表
CREATE TABLE IF NOT EXISTS xianyu_goods (
    id BIGINT PRIMARY KEY,                        -- 表ID（使用雪花ID）
    xy_good_id VARCHAR(100) NOT NULL,             -- 闲鱼商品ID
    xianyu_account_id BIGINT,                     -- 关联的闲鱼账号ID
    title VARCHAR(500),                           -- 商品标题
    cover_pic TEXT,                               -- 封面图片URL
    info_pic TEXT,                                -- 商品详情图片（JSON数组）
    detail_info TEXT,                             -- 商品详情信息（预留字段）
    detail_url TEXT,                              -- 商品详情页URL
    sold_price VARCHAR(50),                       -- 商品价格
    status TINYINT DEFAULT 0,                     -- 商品状态 0:在售 1:已下架 2:已售出
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建商品表索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_goods_xy_good_id ON xianyu_goods(xy_good_id);
CREATE INDEX IF NOT EXISTS idx_goods_status ON xianyu_goods(status);
CREATE INDEX IF NOT EXISTS idx_goods_account_id ON xianyu_goods(xianyu_account_id);

-- 创建商品表更新时间触发器
CREATE TRIGGER IF NOT EXISTS update_xianyu_goods_time
AFTER UPDATE ON xianyu_goods
BEGIN
    UPDATE xianyu_goods SET updated_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END
$

-- 闲鱼聊天消息表
CREATE TABLE IF NOT EXISTS xianyu_chat_message (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    
    -- 关联信息
    xianyu_account_id BIGINT NOT NULL,            -- 关联的闲鱼账号ID
    
    -- WebSocket消息字段
    lwp VARCHAR(50),                              -- websocket消息类型，比如："/s/para"
    pnm_id VARCHAR(100) NOT NULL,                 -- 对应的消息pnmid，比如："3813496236127.PNM"（字段1.3）
    s_id VARCHAR(100),                            -- 消息聊天框id，比如："55435931514@goofish"（字段1.2）
    
    -- 消息内容
    content_type INTEGER,                         -- 消息类别，contentType=1用户消息，32系统消息（字段1.6.3.5中的contentType）
    msg_content TEXT,                             -- 消息内容，对应1.10.reminderContent
    
    -- 发送者信息
    sender_user_name VARCHAR(200),                -- 发送者用户名称，对应1.10.reminderTitle
    sender_user_id VARCHAR(100),                  -- 发送者用户id，对应1.10.senderUserId
    sender_app_v VARCHAR(50),                     -- 发送者app版本，对应1.10._appVersion
    sender_os_type VARCHAR(20),                   -- 发送者系统版本，对应1.10._platform
    
    -- 消息链接
    reminder_url TEXT,                            -- 消息链接，对应1.10.reminderUrl
    xy_goods_id VARCHAR(100),                     -- 闲鱼商品ID，从reminder_url中的itemId参数解析
    
    -- 完整消息体
    complete_msg TEXT NOT NULL,                   -- 完整的消息体JSON
    
    -- 时间信息
    message_time BIGINT,                          -- 消息时间戳（毫秒，字段1.5）
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    
    -- 外键约束
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建聊天消息表索引
CREATE INDEX IF NOT EXISTS idx_chat_message_account_id ON xianyu_chat_message(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_pnm_id ON xianyu_chat_message(pnm_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_s_id ON xianyu_chat_message(s_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_sender_user_id ON xianyu_chat_message(sender_user_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_content_type ON xianyu_chat_message(content_type);
CREATE INDEX IF NOT EXISTS idx_chat_message_time ON xianyu_chat_message(message_time);
CREATE INDEX IF NOT EXISTS idx_chat_message_goods_id ON xianyu_chat_message(xy_goods_id);

-- 创建唯一索引，防止重复消息
CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_message_unique 
ON xianyu_chat_message(xianyu_account_id, pnm_id);

-- 商品配置表
CREATE TABLE IF NOT EXISTS xianyu_goods_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT NOT NULL,                -- 闲鱼账号ID
    xianyu_goods_id BIGINT,                           -- 本地闲鱼商品ID
    xy_goods_id VARCHAR(100) NOT NULL,                -- 闲鱼的商品ID
    xianyu_auto_delivery_on TINYINT DEFAULT 0,        -- 自动发货开关：1-开启，0-关闭，默认关闭
    xianyu_auto_reply_on TINYINT DEFAULT 0,           -- 自动回复开关：1-开启，0-关闭，默认关闭
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,   -- 创建时间
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,   -- 更新时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建商品配置表索引
CREATE INDEX IF NOT EXISTS idx_goods_config_account_id ON xianyu_goods_config(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_goods_config_xy_goods_id ON xianyu_goods_config(xy_goods_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_goods_config_unique ON xianyu_goods_config(xianyu_account_id, xy_goods_id);

-- 创建商品配置表更新时间触发器
CREATE TRIGGER IF NOT EXISTS update_xianyu_goods_config_time
AFTER UPDATE ON xianyu_goods_config
BEGIN
    UPDATE xianyu_goods_config SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END
$

-- 商品自动发货配置表
CREATE TABLE IF NOT EXISTS xianyu_goods_auto_delivery_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT NOT NULL,                -- 闲鱼账号ID
    xianyu_goods_id BIGINT,                           -- 本地闲鱼商品ID
    xy_goods_id VARCHAR(100) NOT NULL,                -- 闲鱼的商品ID
    type TINYINT DEFAULT 1,                           -- 发货类型（1-文本，2-自定义）
    auto_delivery_content TEXT,                       -- 自动发货的文本内容
    auto_confirm_shipment TINYINT DEFAULT 0,          -- 自动确认发货开关：0-关闭，1-开启
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,   -- 创建时间
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,   -- 更新时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建自动发货配置表索引
CREATE INDEX IF NOT EXISTS idx_auto_delivery_config_account_id ON xianyu_goods_auto_delivery_config(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_auto_delivery_config_xy_goods_id ON xianyu_goods_auto_delivery_config(xy_goods_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_auto_delivery_config_unique ON xianyu_goods_auto_delivery_config(xianyu_account_id, xy_goods_id);

-- 创建自动发货配置表更新时间触发器
CREATE TRIGGER IF NOT EXISTS update_xianyu_goods_auto_delivery_config_time
AFTER UPDATE ON xianyu_goods_auto_delivery_config
BEGIN
    UPDATE xianyu_goods_auto_delivery_config SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END
$

-- 商品自动发货记录表
CREATE TABLE IF NOT EXISTS xianyu_goods_auto_delivery_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT NOT NULL,                -- 闲鱼账号ID
    xianyu_goods_id BIGINT,                           -- 本地闲鱼商品ID
    xy_goods_id VARCHAR(100) NOT NULL,                -- 闲鱼的商品ID
    pnm_id VARCHAR(100) NOT NULL,                     -- 消息pnmid，用于防止重复发货
    buyer_user_id VARCHAR(100),                       -- 买家用户ID
    buyer_user_name VARCHAR(100),                     -- 买家用户名称
    content TEXT,                                     -- 发货消息内容
    state TINYINT DEFAULT 0,                          -- 状态是否成功1-成功，0-失败
    order_id VARCHAR(100),                            -- 订单ID
    order_state TINYINT DEFAULT 0,                    -- 确认发货状态：0-未确认发货，1-已确认发货
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,   -- 创建时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建自动发货记录表索引
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_account_id ON xianyu_goods_auto_delivery_record(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_xy_goods_id ON xianyu_goods_auto_delivery_record(xy_goods_id);
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_state ON xianyu_goods_auto_delivery_record(state);
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_create_time ON xianyu_goods_auto_delivery_record(create_time);
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_pnm_id ON xianyu_goods_auto_delivery_record(pnm_id);
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_order_id ON xianyu_goods_auto_delivery_record(order_id);
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_order_state ON xianyu_goods_auto_delivery_record(order_state);

-- 创建唯一索引，防止同一消息重复发货
CREATE UNIQUE INDEX IF NOT EXISTS idx_auto_delivery_record_unique 
ON xianyu_goods_auto_delivery_record(xianyu_account_id, pnm_id);

-- 商品自动回复配置表
CREATE TABLE IF NOT EXISTS xianyu_goods_auto_reply_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT NOT NULL,                -- 闲鱼账号ID
    xianyu_goods_id BIGINT,                           -- 本地闲鱼商品ID
    xy_goods_id VARCHAR(100) NOT NULL,                -- 闲鱼的商品ID
    keyword TEXT,                                     -- 关键词（支持多个，用逗号分隔）
    reply_content TEXT,                               -- 回复内容
    match_type TINYINT DEFAULT 1,                     -- 匹配类型（1-包含，2-完全匹配，3-正则）
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,   -- 创建时间
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,   -- 更新时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建自动回复配置表索引
CREATE INDEX IF NOT EXISTS idx_auto_reply_config_account_id ON xianyu_goods_auto_reply_config(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_auto_reply_config_xy_goods_id ON xianyu_goods_auto_reply_config(xy_goods_id);

-- 创建自动回复配置表更新时间触发器
CREATE TRIGGER IF NOT EXISTS update_xianyu_goods_auto_reply_config_time
AFTER UPDATE ON xianyu_goods_auto_reply_config
BEGIN
    UPDATE xianyu_goods_auto_reply_config SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END
$

-- 商品自动回复记录表
CREATE TABLE IF NOT EXISTS xianyu_goods_auto_reply_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT NOT NULL,                -- 闲鱼账号ID
    xianyu_goods_id BIGINT,                           -- 本地闲鱼商品ID
    xy_goods_id VARCHAR(100) NOT NULL,                -- 闲鱼的商品ID
    buyer_message TEXT,                               -- 买家消息内容
    reply_content TEXT,                               -- 回复消息内容
    matched_keyword VARCHAR(200),                     -- 匹配的关键词
    state TINYINT DEFAULT 0,                          -- 状态是否成功1-成功，0-失败
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,   -- 创建时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建自动回复记录表索引
CREATE INDEX IF NOT EXISTS idx_auto_reply_record_account_id ON xianyu_goods_auto_reply_record(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_auto_reply_record_xy_goods_id ON xianyu_goods_auto_reply_record(xy_goods_id);
CREATE INDEX IF NOT EXISTS idx_auto_reply_record_state ON xianyu_goods_auto_reply_record(state);
CREATE INDEX IF NOT EXISTS idx_auto_reply_record_create_time ON xianyu_goods_auto_reply_record(create_time);
