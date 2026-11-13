CREATE TABLE audit_logs (
 id BIGSERIAL PRIMARY KEY,
 actor_email VARCHAR(150),
 action VARCHAR(60) NOT NULL,
 entity_type VARCHAR(60) NOT NULL,
 entity_id VARCHAR(80),
 details VARCHAR(1000),
 created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);
CREATE TABLE fraud_alerts (
 id BIGSERIAL PRIMARY KEY,
 transaction_id BIGINT NOT NULL UNIQUE REFERENCES bank_transactions(id),
 account_id BIGINT NOT NULL REFERENCES bank_accounts(id),
 severity VARCHAR(20) NOT NULL,
 reason VARCHAR(500) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
 created_at TIMESTAMP NOT NULL,
 resolved_at TIMESTAMP,
 resolved_by_email VARCHAR(150)
);
CREATE INDEX idx_fraud_alerts_status ON fraud_alerts(status, created_at DESC);
CREATE TABLE bank_notifications (
 id BIGSERIAL PRIMARY KEY,
 user_id BIGINT NOT NULL REFERENCES bank_users(id),
 title VARCHAR(120) NOT NULL,
 message VARCHAR(500) NOT NULL,
 is_read BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL,
 read_at TIMESTAMP
);
CREATE INDEX idx_bank_notifications_user_created ON bank_notifications(user_id, created_at DESC);
CREATE TABLE interest_postings (
 id BIGSERIAL PRIMARY KEY,
 account_id BIGINT NOT NULL REFERENCES bank_accounts(id),
 posting_key VARCHAR(40) NOT NULL,
 amount NUMERIC(19,2) NOT NULL,
 posted_at TIMESTAMP NOT NULL,
 CONSTRAINT uk_interest_account_period UNIQUE(account_id, posting_key)
);
