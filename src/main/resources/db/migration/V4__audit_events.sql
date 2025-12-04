CREATE TABLE IF NOT EXISTS audit_event (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES app_user (id) ON DELETE SET NULL,
    event_type TEXT NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    ip INET,
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_event_user_created_at ON audit_event (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_event_type_created_at ON audit_event (event_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_event_created_at ON audit_event (created_at DESC);

CREATE TABLE IF NOT EXISTS app_config (
    id BIGSERIAL PRIMARY KEY,
    config_key TEXT NOT NULL UNIQUE,
    value TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO app_config (config_key, value)
VALUES ('audit.logging.enabled', 'true')
ON CONFLICT (config_key) DO NOTHING;
