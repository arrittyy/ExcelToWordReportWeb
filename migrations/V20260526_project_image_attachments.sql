CREATE TABLE IF NOT EXISTS project_image_attachments (
    id SERIAL PRIMARY KEY,
    project_id INTEGER NOT NULL,
    image_urls TEXT NOT NULL,
    description VARCHAR(500),
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_project_image_attachments_project
        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_project_image_attachments_project_id
    ON project_image_attachments(project_id);
