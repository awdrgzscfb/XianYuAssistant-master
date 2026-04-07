-- 升级脚本：添加自动确认发货开关字段
-- 日期：2025-12-23
-- 说明：在 xianyu_goods_auto_delivery_config 表中添加 auto_confirm_shipment 字段

-- 添加 auto_confirm_shipment 字段（0-关闭，1-开启）
ALTER TABLE xianyu_goods_auto_delivery_config ADD COLUMN auto_confirm_shipment TINYINT DEFAULT 0;

-- 添加注释
COMMENT ON COLUMN xianyu_goods_auto_delivery_config.auto_confirm_shipment IS '自动确认发货开关：0-关闭，1-开启';
