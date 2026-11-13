CREATE TABLE bank_users (
 id BIGSERIAL PRIMARY KEY,
 full_name VARCHAR(100) NOT NULL,
 email VARCHAR(150) NOT NULL UNIQUE,
 password_hash VARCHAR(255) NOT NULL,
 phone VARCHAR(20),
 national_id VARCHAR(30) UNIQUE,
 address VARCHAR(255),
 role VARCHAR(30) NOT NULL,
 kyc_status VARCHAR(20) NOT NULL,
 enabled BOOLEAN NOT NULL DEFAULT TRUE,
 kyc_review_note VARCHAR(500),
 kyc_reviewed_at TIMESTAMP,
 kyc_reviewed_by_email VARCHAR(150),
 created_at TIMESTAMP NOT NULL
);
CREATE TABLE bank_accounts (
 id BIGSERIAL PRIMARY KEY,
 account_number VARCHAR(30) NOT NULL UNIQUE,
 annual_interest_rate NUMERIC(5,2) NOT NULL,
 balance NUMERIC(19,2) NOT NULL,
 created_at TIMESTAMP NOT NULL,
 minimum_balance NUMERIC(19,2) NOT NULL,
 customer_id BIGINT NOT NULL REFERENCES bank_users(id),
 status VARCHAR(20) NOT NULL,
 version BIGINT,
 account_type VARCHAR(30) NOT NULL,
 tenure_months INTEGER
);
CREATE INDEX idx_bank_accounts_customer ON bank_accounts(customer_id);
CREATE INDEX idx_bank_accounts_status ON bank_accounts(status);
CREATE TABLE bank_transactions (
 id BIGSERIAL PRIMARY KEY,
 reference VARCHAR(36) NOT NULL UNIQUE,
 type VARCHAR(20) NOT NULL,
 source_account_id BIGINT REFERENCES bank_accounts(id),
 destination_account_id BIGINT REFERENCES bank_accounts(id),
 amount NUMERIC(19,2) NOT NULL,
 description VARCHAR(255),
 created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_bank_transactions_created ON bank_transactions(created_at DESC);
CREATE INDEX idx_bank_transactions_source ON bank_transactions(source_account_id);
CREATE INDEX idx_bank_transactions_destination ON bank_transactions(destination_account_id);
