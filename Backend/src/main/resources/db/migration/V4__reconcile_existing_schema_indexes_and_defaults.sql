-- Reconcile the existing Batch 1-6 Supabase schema with the indexes and
-- database defaults declared by V1-V3. Safe for the already-populated schema.

-- V1: core banking indexes
CREATE INDEX IF NOT EXISTS idx_bank_accounts_customer
    ON bank_accounts(customer_id);
CREATE INDEX IF NOT EXISTS idx_bank_accounts_status
    ON bank_accounts(status);
CREATE INDEX IF NOT EXISTS idx_bank_transactions_created
    ON bank_transactions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_bank_transactions_source
    ON bank_transactions(source_account_id);
CREATE INDEX IF NOT EXISTS idx_bank_transactions_destination
    ON bank_transactions(destination_account_id);

-- V2: loan indexes
CREATE INDEX IF NOT EXISTS idx_bank_loans_applicant
    ON bank_loans(applicant_id);
CREATE INDEX IF NOT EXISTS idx_bank_loans_status
    ON bank_loans(status);
CREATE INDEX IF NOT EXISTS idx_loan_repayments_loan
    ON loan_repayments(loan_id, installment_number);

-- V3: audit, fraud and notification indexes
CREATE INDEX IF NOT EXISTS idx_audit_logs_created
    ON audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_status
    ON fraud_alerts(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_bank_notifications_user_created
    ON bank_notifications(user_id, created_at DESC);

-- Restore defaults that were absent from the Hibernate-created tables.
ALTER TABLE bank_users
    ALTER COLUMN enabled SET DEFAULT TRUE;
ALTER TABLE fraud_alerts
    ALTER COLUMN status SET DEFAULT 'OPEN';
ALTER TABLE bank_notifications
    ALTER COLUMN is_read SET DEFAULT FALSE;
