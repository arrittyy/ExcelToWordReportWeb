-- Add project_type to projects table.
-- Run against production after backup.

ALTER TABLE public.projects
    ADD COLUMN IF NOT EXISTS project_type character varying(50);

COMMENT ON COLUMN public.projects.project_type IS '项目类型：金属监督、防磨防爆、锅炉内检、锅炉外检、容器定检、容器外检';

