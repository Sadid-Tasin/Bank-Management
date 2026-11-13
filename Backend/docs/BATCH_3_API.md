# Batch 3 — Employee and Admin API

All routes require a valid Bearer JWT. Method-level role checks are enforced by Spring Security.

## Employee customer directory

- `GET /api/v1/employee/customers?page=0&size=20`
  - Roles: `TELLER`, `LOAN_OFFICER`, `MANAGER`, `ADMIN`
  - Returns a paginated customer directory. National ID and address are visible only to authenticated employee roles.

## KYC review

- `GET /api/v1/employee/kyc/pending?page=0&size=20`
- `GET /api/v1/employee/kyc/{customerId}`
- `PUT /api/v1/employee/kyc/{customerId}`
  - Roles: `TELLER`, `MANAGER`, `ADMIN`
  - Request body:

```json
{
  "status": "VERIFIED",
  "note": "Identity documents checked"
}
```

  - Allowed decisions: `VERIFIED` or `REJECTED`. A KYC request can only be decided while its current status is `PENDING`. Reviewer email, review time, and note are stored with the customer record.

## Teller counter transactions

- `POST /api/v1/employee/counter/deposit`
- `POST /api/v1/employee/counter/withdraw`
  - Roles: `TELLER`, `MANAGER`, `ADMIN`
  - Request body:

```json
{
  "accountNumber": "31000000000000",
  "amount": 500.00,
  "description": "Cash received at counter"
}
```

  - Only verified customer-owned accounts can be used. The transaction description includes the handling employee identity.

## Account controls

- `PUT /api/v1/employee/accounts/{accountNumber}/freeze`
- `PUT /api/v1/employee/accounts/{accountNumber}/unfreeze`
  - Roles: `MANAGER`, `ADMIN`
  - Freezing/unfreezing is performed under a database write lock. Closed accounts cannot be frozen; only frozen accounts can be unfrozen.

## Admin staff management

- `POST /api/v1/admin/staff`
- `GET /api/v1/admin/staff?page=0&size=20`
- `PATCH /api/v1/admin/staff/{staffId}/role`
- `PATCH /api/v1/admin/staff/{staffId}/status`
  - Role: `ADMIN` only.
  - Staff roles managed by these endpoints: `TELLER`, `LOAN_OFFICER`, `MANAGER`.
  - Public/admin staff APIs cannot create another `ADMIN` or convert a staff member/customer to `ADMIN`. Initial administrator provisioning is intentionally reserved for the one-time bootstrap step in Batch 6.
  - Passwords are stored using BCrypt. Staff deactivation and role changes are reflected immediately in JWT authorization because current user status/role is reloaded from the database for each authenticated request.

## Not included in Batch 3

Loan applications/approvals and EMI repayment are Batch 4. Audit trails, fraud checks, interest posting, reports, and statement exports are Batch 5. Database versioned migrations and initial administrator bootstrap are Batch 6.
