ALTER TABLE projects

    ADD COLUMN IF NOT EXISTS third_party_approval_by_experiment_type jsonb;


