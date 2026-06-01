-- 项目总报告：用户上传的通知单签字版、第三方完整版（相对 uploads 根目录的路径 + 原始文件名）
ALTER TABLE projects ADD COLUMN IF NOT EXISTS summary_notification_signed_rel_path VARCHAR(500);
ALTER TABLE projects ADD COLUMN IF NOT EXISTS summary_notification_signed_original_name VARCHAR(255);
ALTER TABLE projects ADD COLUMN IF NOT EXISTS summary_third_party_full_rel_path VARCHAR(500);
ALTER TABLE projects ADD COLUMN IF NOT EXISTS summary_third_party_full_original_name VARCHAR(255);
