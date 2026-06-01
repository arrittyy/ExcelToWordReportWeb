CREATE TABLE IF NOT EXISTS word_export_job (id varchar(36) PRIMARY KEY,project_id integer NOT NULL,creator_user_id varchar(255) NOT NULL,type varchar(64) NOT NULL,payload text,status varchar(32) NOT NULL,output_rel_path varchar(1000),suggested_file_name varchar(500) NOT NULL,error_message text,created_at timestamp NOT NULL DEFAULT now(),started_at timestamp,finished_at timestamp);

CREATE INDEX IF NOT EXISTS idx_word_export_job_project_id ON word_export_job(project_id);
CREATE INDEX IF NOT EXISTS idx_word_export_job_creator_user_id ON word_export_job(creator_user_id);
CREATE INDEX IF NOT EXISTS idx_word_export_job_status_created_at ON word_export_job(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_word_export_job_finished_at ON word_export_job(finished_at);
