ALTER TABLE projects
    ADD COLUMN IF NOT EXISTS ndt_signature_levels jsonb;
