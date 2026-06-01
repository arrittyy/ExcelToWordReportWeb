ALTER TABLE projects
    ADD COLUMN IF NOT EXISTS aggregate_detection_log_order TEXT;
