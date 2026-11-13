# Backend Batches 4–6 — Loans, Controls, Reporting and Production Setup

All protected routes require `Authorization: Bearer <access-token>`. JSON request bodies use the property names shown below. Amounts are BDT and must contain at most two decimal places.

## Batch 4 — Loan management

### Customer loan routes

- `POST /api/v1/loans` — `CUSTOMER`
- `GET /api/v1/loans?page=0&size=20` — `CUSTOMER`, own loans only
- `GET /api/v1/loans/{loanId}` — `CUSTOMER`, own loan only
- `GET /api/v1/loans/{loanId}/repayments` — `CUSTOMER`, own repayment history only
- `POST /api/v1/loans/{loanId}/repayments` — `CUSTOMER`, pay the next installment

Application example:

```json
{
  "loanType": "EDUCATION",
  "purpose": "University tuition and study expenses",
  "principalAmount": 150000.00,
  "termMonths": 24,
  "accountNumber": "10000000000000"
}
```

Repayment example:

```json
{
  "accountNumber": "10000000000000",
  "amount": 7061.25
}
```

Eligibility rules: verified customer KYC; active Savings or Current disbursement account owned by the applicant; principal BDT 1,000–5,000,000; term 3–84 months. Fixed Deposit accounts cannot receive loan disbursements.

### Loan Officer routes

- `GET /api/v1/employee/loans/pending?page=0&size=20`
- `POST /api/v1/employee/loans/{loanId}/decision`
- Roles: `LOAN_OFFICER`, `MANAGER`, `ADMIN`.

Approval example:

```json
{
  "approved": true,
  "annualInterestRate": 10.50,
  "note": "Income documents reviewed"
}
```

Rejection example:

```json
{
  "approved": false,
  "note": "Required supporting documents are missing"
}
```

Approval calculates a fixed monthly installment, credits the principal to the selected account, and changes the loan to `DISBURSED` in the same database transaction. If the credit fails, the approval is rolled back. Rejected loans are not disbursed. Repayments must match the next installment; the final installment adjusts for rounding. All repayment withdrawals and loan repayments are recorded.

## Batch 5 — Audit, fraud monitoring, interest and notifications

### Customer/staff notifications

- `GET /api/v1/notifications?page=0&size=20` — current user's notifications only
- `PATCH /api/v1/notifications/{id}/read` — mark own notification as read

Notifications are in-app records. This release does not send email or SMS.

### Admin reports and operations

- `GET /api/v1/admin/reports/overview` — KPI summary
- `GET /api/v1/admin/reports/transactions?page=0&size=20` — paginated transaction ledger view
- `GET /api/v1/admin/audit-logs?page=0&size=20` — recent audit events
- `GET /api/v1/admin/fraud-alerts?page=0&size=20` — open fraud alerts
- `PATCH /api/v1/admin/fraud-alerts/{id}/resolve` — resolve an alert
- `POST /api/v1/admin/interest/post?period=YYYY-MM` — manually post interest for the current or previous month
- All listed admin operations require `ADMIN`.

Fraud monitoring creates a review alert for transactions of BDT 100,000 or above, or when an account reaches five or more transactions in a rolling ten-minute window. Alerts are for review; this academic project does not automatically freeze funds or block transactions.

Savings interest is posted monthly by a scheduled job at 02:00 Asia/Dhaka on the first day of the month, for the previous month. Fixed Deposit interest is posted once after maturity. A unique account/period key prevents duplicate postings. Current Accounts do not accrue interest in this model.

## Batch 6 — Production configuration

### Database migrations

Flyway migrations live in `src/main/resources/db/migration`:

- `V1__core_banking_schema.sql` — base user/account/transaction tables for a new empty database
- `V2__loan_management.sql` — loans and repayments
- `V3__audit_fraud_interest_notifications.sql` — audit, fraud, interest and notifications

`spring.flyway.baseline-on-migrate=true` baselines an existing non-empty schema at version 1 and then runs V2/V3. New empty databases run V1–V3. Hibernate is switched to `ddl-auto=validate`; schema changes should be made through new Flyway migration files, not Hibernate auto-update.

**Before first production deployment:** take a Supabase backup, verify the current tables/columns, confirm that the existing database has the Batch 3 schema (including the KYC review columns), run the migration with the local test environment first, then deploy. Do not delete or recreate existing tables.

### Required Render environment variables

Keep secrets in Render Environment settings; never commit them:

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL=jdbc:postgresql://<supabase-session-pooler-host>:5432/postgres?sslmode=require`
- `SPRING_DATASOURCE_USERNAME=<supabase-pooler-username>`
- `SPRING_DATASOURCE_PASSWORD=<supabase-database-password>`
- `APP_JWT_SECRET=<base64-encoded-random-secret-at-least-32-bytes>`
- `APP_ADMIN_EMAIL=<initial-admin-email>`
- `APP_ADMIN_PASSWORD=<initial-admin-password-at-least-12-characters>`
- `APP_ADMIN_NAME=<initial-admin-display-name>` (optional)
- `APP_CORS_ALLOWED_ORIGINS=https://<your-vercel-domain>` (comma-separated if there is more than one frontend origin)

At startup the bootstrap runner creates the first Admin only when both Admin email and password are configured and that email is unused. It is idempotent and does not reset an existing Admin password. It refuses to promote an existing non-admin user. Remove `APP_ADMIN_PASSWORD` from Render after the first successful bootstrap/restart if you do not want it retained as an environment value; the created BCrypt hash remains in the database.

### Health check

- `GET /actuator/health` — public, details hidden; use this as the Render health-check path.

CORS allows only the configured origins, common API methods and Bearer-token headers. Cookie-based authentication is not enabled.

## Important project scope

This is an academic banking simulation, not a production financial platform. It has no payment gateway, external bank rails, SMS/email provider, credit-bureau verification, regulatory compliance engine, or automated real-world fraud interdiction. Loan interest/repayment and interest posting are simplified demonstrable models. Production use would require independent security, accounting, legal, operational and financial review.
