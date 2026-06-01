-- 可选：已有数据库升级时执行一次（新建库请使用 reportweb.sql 完整建表）
-- 新增「项目编号（第三方）」列，与内部 project_number 独立、可空、不参与唯一性校验
ALTER TABLE public.projects
    ADD COLUMN IF NOT EXISTS third_party_project_number character varying(100);
