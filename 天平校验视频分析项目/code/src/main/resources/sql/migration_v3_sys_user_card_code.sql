-- 旧库升级参考；应用启动时 DataInitializer 也会自动检测并补充该列。
ALTER TABLE sys_user ADD COLUMN card_code VARCHAR(20) NULL AFTER real_name;

UPDATE sys_user SET card_code = 'JY001'
WHERE username = 'liyi' AND (card_code IS NULL OR TRIM(card_code) = '');
UPDATE sys_user SET card_code = 'JY002'
WHERE username = 'lier' AND (card_code IS NULL OR TRIM(card_code) = '');
UPDATE sys_user SET card_code = 'JY003'
WHERE username = 'qiyi' AND (card_code IS NULL OR TRIM(card_code) = '');
