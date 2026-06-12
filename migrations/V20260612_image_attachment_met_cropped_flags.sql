-- 金相 MET 附图：标记是否经过标尺裁剪
ALTER TABLE image_attachments
    ADD COLUMN IF NOT EXISTS met_cropped_flags TEXT;
