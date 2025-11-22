CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA public;

CREATE TABLE IF NOT EXISTS sector (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL
);

INSERT INTO sector (name)
SELECT name FROM (VALUES
    ('Barreales'),
    ('Chépica'),
    ('Los Boldos'),
    ('Palmilla'),
    ('Quinahue'),
    ('Santa Cruz')
) AS seeds(name)
WHERE NOT EXISTS (SELECT 1 FROM sector);

CREATE TABLE IF NOT EXISTS customer (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    sector_id BIGINT NOT NULL,
    address TEXT NOT NULL,
    debt INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_customer_sector FOREIGN KEY (sector_id) REFERENCES sector (id)
);

CREATE TABLE IF NOT EXISTS statistic (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    amount INTEGER NOT NULL,
    sale_type VARCHAR(32),
    items_count INTEGER,
    date DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS transaction (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    date DATE NOT NULL,
    detail TEXT NOT NULL,
    amount INTEGER NOT NULL,
    balance INTEGER NOT NULL,
    type VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    item_count INTEGER,
    CONSTRAINT fk_transaction_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
);
