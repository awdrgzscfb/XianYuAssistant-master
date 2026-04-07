-- 为自动发货记录表添加订单相关字段
-- 执行时间：2025-01-15

-- 添加 order_id 字段
ALTER TABLE xianyu_goods_auto_delivery_record ADD COLUMN order_id VARCHAR(100);

-- 添加 order_state 字段 (1-已发货，0-未发货，-1-自动确认发货失败)
ALTER TABLE xianyu_goods_auto_delivery_record ADD COLUMN order_state TINYINT DEFAULT 0;

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_order_id ON xianyu_goods_auto_delivery_record(order_id);
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_order_state ON xianyu_goods_auto_delivery_record(order_state);

-- 添加注释
COMMENT ON COLUMN xianyu_goods_auto_delivery_record.order_id IS '订单ID';
COMMENT ON COLUMN xianyu_goods_auto_delivery_record.order_state IS '订单状态：1-已发货，0-未发货，-1-自动确认发货失败';
