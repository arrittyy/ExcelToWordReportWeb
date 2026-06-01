-- Increase reports.inspector length to support multi-select inspector names.
-- Run after backup.

ALTER TABLE public.reports
    ALTER COLUMN inspector TYPE character varying(300);

