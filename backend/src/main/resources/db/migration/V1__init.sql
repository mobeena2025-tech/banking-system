CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    phone         VARCHAR(20),
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE accounts (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20)    NOT NULL,
    user_id        BIGINT         NOT NULL,
    type           VARCHAR(20)    NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    balance        DECIMAL(19, 2) NOT NULL,
    created_at     TIMESTAMP(6)   NOT NULL,
    version        BIGINT         NOT NULL,
    CONSTRAINT uk_accounts_number UNIQUE (account_number),
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE transactions (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference          VARCHAR(40)    NOT NULL,
    type               VARCHAR(20)    NOT NULL,
    amount             DECIMAL(19, 2) NOT NULL,
    from_account_id    BIGINT,
    to_account_id      BIGINT,
    from_balance_after DECIMAL(19, 2),
    to_balance_after   DECIMAL(19, 2),
    description        VARCHAR(255),
    initiated_by       BIGINT         NOT NULL,
    idempotency_key    VARCHAR(64),
    created_at         TIMESTAMP(6)   NOT NULL,
    CONSTRAINT uk_transactions_reference UNIQUE (reference),
    CONSTRAINT uk_transactions_idempotency UNIQUE (initiated_by, idempotency_key),
    CONSTRAINT fk_tx_from FOREIGN KEY (from_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_tx_to FOREIGN KEY (to_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_tx_user FOREIGN KEY (initiated_by) REFERENCES users (id)
);

CREATE INDEX idx_tx_from ON transactions (from_account_id, created_at);
CREATE INDEX idx_tx_to ON transactions (to_account_id, created_at);
