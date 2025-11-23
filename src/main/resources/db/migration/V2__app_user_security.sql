CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    role VARCHAR(16) NOT NULL,
    pin_hash CHAR(64) NOT NULL,
    pin_salt CHAR(16) NOT NULL,
    pin_fingerprint CHAR(64) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_app_user_role ON app_user (role);

INSERT INTO app_user (name, role, pin_hash, pin_salt, pin_fingerprint)
VALUES
    ('Administrador', 'ADMIN', '39f3a563f18cc784297b820fd271db2f0e3e1da6330333e3afa8d1dda95d9cc7', 'a1b2c3d4e5f60708', '0ffe1abd1a08215353c233d6e009613e95eec4253832a761af28ff37ac5a150c'),
    ('Vendedor', 'NORMAL', '154b36cf116dc0c09ce4f7727916a396e880b3d27a154cecc9373f668fbd21b5', '0f1e2d3c4b5a6978', 'edee29f882543b956620b26d0ee0e7e950399b1c4222f5de05e06425b4c995e9')
ON CONFLICT (pin_fingerprint) DO NOTHING;
