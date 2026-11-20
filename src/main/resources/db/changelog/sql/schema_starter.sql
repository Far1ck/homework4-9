--liquibase formatted sql

--changeset dev:1-create-clients
CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL CHECK (char_length(trim(name)) > 0),
    email VARCHAR(255) NOT NULL UNIQUE CHECK (char_length(trim(email)) > 0)
);

CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    account_number VARCHAR(34) NOT NULL UNIQUE CHECK (char_length(trim(account_number)) > 0),
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    CONSTRAINT fk_accounts_client FOREIGN KEY (client_id) REFERENCES clients(id)
);

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('TRANSFER_IN', 'TRANSFER_OUT')),
    transaction_date TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE INDEX idx_transactions_account_id ON transactions(account_id);
