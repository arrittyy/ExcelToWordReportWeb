-- 可选：已有数据库升级时执行一次（新建库请使用 reportweb.sql 完整建表）
-- 新增「第三方名称」列，可空
ALTER TABLE public.projects
    ADD COLUMN IF NOT EXISTS third_party_name character varying(200);
