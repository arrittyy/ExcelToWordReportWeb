ALTER TABLE public.material_library_entry
    ADD COLUMN IF NOT EXISTS modification_type VARCHAR(20) NOT NULL DEFAULT 'CREATE';

ALTER TABLE public.material_library_entry
    ADD COLUMN IF NOT EXISTS approved_snapshot JSONB;

-- 系统导入记录允许无具体提交人
ALTER TABLE public.material_library_entry
    ALTER COLUMN submitted_by_user_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_material_library_entry_source
    ON public.material_library_entry (source);

CREATE INDEX IF NOT EXISTS idx_material_library_entry_submitted_by
    ON public.material_library_entry (submitted_by_user_id, created_at DESC);
