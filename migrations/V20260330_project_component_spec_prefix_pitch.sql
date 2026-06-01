-- 部件规格前缀（PHI|M|NONE，NULL=按名称自动）与可选牙距
-- PostgreSQL：列注释使用 COMMENT ON COLUMN，不支持 ADD COLUMN ... COMMENT / AFTER

ALTER TABLE public.project_components
    ADD COLUMN IF NOT EXISTS spec_prefix character varying(8) NULL,
    ADD COLUMN IF NOT EXISTS thread_pitch character varying(50) NULL;

COMMENT ON COLUMN public.project_components.spec_prefix IS 'PHI|M|NONE; NULL=auto by name';
COMMENT ON COLUMN public.project_components.thread_pitch IS 'optional thread pitch';
