CREATE TABLE IF NOT EXISTS public.material_library_entry (
    id BIGSERIAL PRIMARY KEY,
    material_key VARCHAR(100) NOT NULL,
    primary_category VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'USER',
    properties JSONB NOT NULL DEFAULT '{}',
    submitted_by_user_id VARCHAR(450) NOT NULL,
    submitted_by_user_name VARCHAR(200),
    reviewed_by_user_id VARCHAR(450),
    reviewed_by_user_name VARCHAR(200),
    review_comment TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_material_library_entry_material_key
    ON public.material_library_entry (LOWER(material_key));

CREATE INDEX IF NOT EXISTS idx_material_library_entry_status
    ON public.material_library_entry (status);

CREATE INDEX IF NOT EXISTS idx_material_library_entry_category_status
    ON public.material_library_entry (primary_category, status);

CREATE TABLE IF NOT EXISTS public.material_approval_log (
    id BIGSERIAL PRIMARY KEY,
    entry_id BIGINT NOT NULL REFERENCES public.material_library_entry(id) ON DELETE CASCADE,
    action VARCHAR(20) NOT NULL,
    actor_user_id VARCHAR(450) NOT NULL,
    actor_user_name VARCHAR(200),
    comment TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_material_approval_log_entry_created
    ON public.material_approval_log (entry_id, created_at DESC);

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.material_library_entry TO reportweb;
GRANT USAGE, SELECT ON SEQUENCE material_library_entry_id_seq TO reportweb;
GRANT SELECT, INSERT ON TABLE public.material_approval_log TO reportweb;
GRANT USAGE, SELECT ON SEQUENCE material_approval_log_id_seq TO reportweb;
