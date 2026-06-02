CREATE TABLE IF NOT EXISTS public.project_report_change_log (
    id BIGSERIAL PRIMARY KEY,
    project_id INTEGER NOT NULL,
    report_id INTEGER NOT NULL,
    action VARCHAR(20) NOT NULL,
    experiment_type_id INTEGER NOT NULL,
    experiment_type_name VARCHAR(200) NOT NULL,
    experiment_type_code VARCHAR(20) NOT NULL,
    report_number VARCHAR(50),
    test_method VARCHAR(200),
    status VARCHAR(20),
    change_summary JSONB,
    operator_user_id VARCHAR(450) NOT NULL,
    operator_user_name VARCHAR(200),
    source VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_project_report_change_log_project_created
    ON public.project_report_change_log (project_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_project_report_change_log_project_experiment_type
    ON public.project_report_change_log (project_id, experiment_type_id);

GRANT SELECT, INSERT ON TABLE public.project_report_change_log TO reportweb;
GRANT USAGE, SELECT ON SEQUENCE project_report_change_log_id_seq TO reportweb;
