CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE IF NOT EXISTS sector (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

INSERT INTO sector (name) VALUES
    ('Santa Cruz'),
    ('Los Boldos'),
    ('Barreales'),
    ('Palmilla'),
    ('Quinahue'),
    ('Chépica')
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS customer (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    sector_id BIGINT NOT NULL REFERENCES sector(id),
    address TEXT NOT NULL,
    debt INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS transaction (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    detail TEXT NOT NULL,
    amount INTEGER NOT NULL,
    balance INTEGER NOT NULL,
    type VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    item_count INTEGER,
    backup_detail TEXT
);

CREATE INDEX IF NOT EXISTS idx_transaction_customer_date ON transaction (customer_id, date);
CREATE INDEX IF NOT EXISTS idx_transaction_customer_created_at ON transaction (customer_id, created_at);
CREATE INDEX IF NOT EXISTS idx_transaction_type ON transaction (type);

CREATE TABLE IF NOT EXISTS statistic (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    amount INTEGER NOT NULL,
    sale_type VARCHAR(32),
    items_count INTEGER,
    date DATE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_statistic_date ON statistic (date);
CREATE INDEX IF NOT EXISTS idx_statistic_type ON statistic (type);
