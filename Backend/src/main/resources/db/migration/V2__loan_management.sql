CREATE TABLE bank_loans (
 id BIGSERIAL PRIMARY KEY,
 loan_number VARCHAR(36) NOT NULL UNIQUE,
 applicant_id BIGINT NOT NULL REFERENCES bank_users(id),
 linked_account_id BIGINT NOT NULL REFERENCES bank_accounts(id),
 loan_type VARCHAR(30) NOT NULL,
 purpose VARCHAR(500) NOT NULL,
 principal_amount NUMERIC(19,2) NOT NULL,
 annual_interest_rate NUMERIC(5,2),
 term_months INTEGER NOT NULL,
 status VARCHAR(20) NOT NULL,
 monthly_installment NUMERIC(19,2),
 total_payable NUMERIC(19,2),
 outstanding_amount NUMERIC(19,2),
 submitted_at TIMESTAMP NOT NULL,
 reviewed_at TIMESTAMP,
 reviewed_by_email VARCHAR(150),
 decision_note VARCHAR(500),
 disbursed_at TIMESTAMP,
 next_due_date DATE,
 version BIGINT
);
CREATE INDEX idx_bank_loans_applicant ON bank_loans(applicant_id);
CREATE INDEX idx_bank_loans_status ON bank_loans(status);
CREATE TABLE loan_repayments (
 id BIGSERIAL PRIMARY KEY,
 reference VARCHAR(36) NOT NULL UNIQUE,
 loan_id BIGINT NOT NULL REFERENCES bank_loans(id),
 paying_account_id BIGINT NOT NULL REFERENCES bank_accounts(id),
 amount NUMERIC(19,2) NOT NULL,
 installment_number INTEGER NOT NULL,
 paid_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_loan_repayments_loan ON loan_repayments(loan_id, installment_number);
