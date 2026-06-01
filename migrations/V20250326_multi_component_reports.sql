-- Multi-select project components per report: store ordered IDs in JSONB.
-- Run against production after backup.

ALTER TABLE public.reports
    ADD COLUMN IF NOT EXISTS project_component_ids jsonb;

ALTER TABLE public.reports
    ALTER COLUMN component_spec TYPE character varying(500);

COMMENT ON COLUMN public.reports.project_component_ids IS 'Ordered list of project_components.id (same component name, merged specs in app)';
