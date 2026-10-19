-- 已有 tpjy 数据库升级：保留天平实际显示值与单位。
-- 新库的 schema.sql 已包含这两列；旧库只需执行一次本文件。
ALTER TABLE reading ADD COLUMN display_value VARCHAR(50) NULL AFTER reading_value;
ALTER TABLE reading ADD COLUMN display_unit VARCHAR(10) NULL DEFAULT 'g' AFTER display_value;
UPDATE reading SET display_unit = 'g' WHERE display_unit IS NULL;
UPDATE reading SET display_value = CAST(reading_value AS CHAR) WHERE display_value IS NULL AND reading_value IS NOT NULL;
