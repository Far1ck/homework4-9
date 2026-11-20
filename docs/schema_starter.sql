-- MiniBank — стартовая схема для модуля 4.
--
-- Это ЧЕРНОВИК, а не готовое решение — в нём намеренно не хватает
-- нескольких вещей, которые нужно добавить самостоятельно:
--   - FOREIGN KEY между таблицами
--   - UNIQUE на email клиента
--   - индекс(ы) под запрос "все транзакции счёта за период"
--
-- Задание модуля 4 — не просто скопировать это, а доработать и
-- объяснить, почему каждое добавленное ограничение/индекс нужен.

CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL
);

-- Добавлена проверка, что name и email не пустые, не состоят из пробелов
-- email должен быть уникальным, так как одим ящиком обычно пользуется один человек
CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL CHECK (char_length(trim(name)) > 0),
    email VARCHAR(255) NOT NULL UNIQUE CHECK (char_length(trim(email)) > 0)
);

CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    account_number VARCHAR(34) NOT NULL,
    balance NUMERIC(19,4) NOT NULL DEFAULT 0
);

-- Счет должен быть уникальным и не пустым
-- Баланс не может быть меньше нуля
-- Добавлен внешний ключ, приязывающий счет к клиенту
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
    amount NUMERIC(19,4) NOT NULL,
    type VARCHAR(20) NOT NULL,
    transaction_date TIMESTAMP NOT NULL DEFAULT now()
);

-- Добавлено ограничение возможных значений типа операции, думаю тут все понятно
-- Ну и добавлен внешний ключ, чтобы привязать транзакцию к счету.
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('TRANSFER_IN', 'TRANSFER_OUT')),
    transaction_date TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

-- Индекс добавлен по заданию для EXPLAIN ANALYZE, а так в программе он пока не используется, он лишний
CREATE INDEX idx_transactions_account_id ON transactions(account_id);
