ALTER TABLE users ADD COLUMN draft_task_title TEXT;
ALTER TABLE users ADD COLUMN task_creation_return_state TEXT;

ALTER TABLE tasks ADD COLUMN kind TEXT NOT NULL DEFAULT 'PLAN'
    CHECK (kind IN ('PLAN', 'CUSTOM'));

CREATE INDEX tasks_user_kind_status ON tasks(user_id, kind, status);

CREATE TABLE processed_deliveries (
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    delivery_id TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(user_id, delivery_id)
);
