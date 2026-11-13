# Confused Bank — Frontend Integration

This release adds the Next.js frontend under `frontend/` and connects it to the existing Spring Boot REST API. The Java backend, Flyway migrations, Supabase schema and database settings are unchanged.

## Technology

- Next.js App Router + React + TypeScript
- Tailwind CSS 4 (existing project setup) and the approved Confused Bank light theme
- Lucide React icons and Recharts
- Spring Boot REST API with JWT bearer authentication

## Run locally

```bash
cd frontend
npm ci
cp .env.example .env.local
npm run dev
```

Open `http://localhost:3000`. The default API URL is the current Render service. You can override it in `frontend/.env.local`:

```env
NEXT_PUBLIC_API_BASE_URL=https://bank-management-system-m3th.onrender.com
```

Available checks:

```bash
npm run typecheck
npm run lint
npm run build
```

## Deploy the frontend

Create a Vercel project from the existing GitHub repository and set **Root Directory** to `frontend`. Add this Vercel environment variable:

- `NEXT_PUBLIC_API_BASE_URL=https://bank-management-system-m3th.onrender.com`

After the first Vercel deployment, copy the deployed frontend origin (for example `https://your-project.vercel.app`) and update the Render service environment variable `APP_CORS_ALLOWED_ORIGINS` to include both local development and that exact origin, comma-separated, without a trailing slash:

```text
http://localhost:3000,https://your-project.vercel.app
```

Save and redeploy/restart the Render backend after changing the CORS variable. Do not set `APP_CORS_ALLOWED_ORIGINS=*`; the backend intentionally uses an origin allowlist. The browser frontend does not connect directly to Supabase and does not need database credentials.

## Authentication and session handling

- Login: `POST /api/v1/auth/login`
- Profile validation: `GET /api/v1/auth/me`
- Customer registration: `POST /api/v1/auth/register`
- Profile update: `PUT /api/v1/auth/me`
- The access token is held in browser `sessionStorage` for the current tab session and attached as `Authorization: Bearer <token>` to protected calls.
- The session is cleared when its returned `expiresInSeconds` TTL elapses or the API returns HTTP 401.
- No refresh-token or cookie endpoint exists in the supplied backend, so this frontend does not invent one.

## Implemented API-backed areas

| UI area | API routes used | Backend role support |
|---|---|---|
| Dashboard | `/api/v1/admin/reports/overview`, reports transactions, customer accounts/loans, staff directory, KYC and notifications | role-specific |
| Customers | `GET /api/v1/employee/customers` | TELLER, LOAN_OFFICER, MANAGER, ADMIN |
| KYC review | GET pending/customer detail; PUT decision | TELLER, MANAGER, ADMIN |
| Customer accounts | GET/POST `/api/v1/accounts`; GET account transactions | CUSTOMER only |
| Banking | Customer deposit/withdraw/transfer; employee counter deposit/withdraw | CUSTOMER; TELLER/MANAGER/ADMIN for counter operations |
| Account controls | PUT freeze/unfreeze | MANAGER, ADMIN |
| Transactions | Admin reports ledger; customer-owned account ledger | ADMIN or CUSTOMER |
| Loans | Customer applications/own loans/repayments; employee pending and decision | CUSTOMER; LOAN_OFFICER/MANAGER/ADMIN for review |
| Staff management | GET/POST staff; PATCH role/status | ADMIN |
| Reports | Overview, transaction report, monthly interest posting | ADMIN |
| Fraud alerts | GET open alerts; PATCH resolve | ADMIN |
| Audit logs | GET audit logs | ADMIN |
| Notifications | GET own notifications; PATCH mark read | authenticated users |
| Profile | GET/PUT `/api/v1/auth/me` | authenticated users |

## Backend contract boundaries intentionally respected

The supplied backend does **not** expose a global account-list/detail endpoint for ADMIN or staff. The account controller is customer-only, so the Admin UI does not fabricate a bank-wide account directory. Admin overview does expose aggregate active-account count and total account balance.

The supplied banking controller allows customer-owned transfers only. Employee counter APIs support deposits and withdrawals, not employee-initiated transfers. The UI follows these permissions.

There is no KYC document upload endpoint, external payment gateway, email/SMS integration, customer credit check, or customer transaction export endpoint in the supplied source. Those are not represented as working backend features.

List pages request the first API page (generally 50 records; staff 100). The backend pagination contract is retained, but a cross-module paginator has not been added in this release. Search filters the currently loaded page.

## Important production note

This project is an academic banking simulation as described in the backend API documentation, not a production financial system. The UI enforces role-aware navigation, but the backend remains the authority for access control and validation. Production use needs an independent security review; sessionStorage bearer-token storage is a pragmatic integration choice for this existing token API, not a substitute for an HttpOnly-cookie session architecture.

## Verification performed for this release archive

- All frontend TypeScript/TSX source files passed a TypeScript syntax/transpilation diagnostic.
- The supplied Java backend sources and migrations were retained unchanged.
- A full `npm ci`, Next.js production build, and ESLint run could not be completed in the packaging environment because access to `registry.npmjs.org` failed with a DNS/network `EAI_AGAIN` error. Run the commands above on a machine with npm registry access before production deployment. Do not treat this archive as a fully production-build-verified release until those checks pass.


## Production deployment

See `DEPLOYMENT_VERCEL_CLOUDFLARE.md` for the GitHub push sequence, Vercel `frontend/` root-directory settings, Cloudflare CNAME configuration for `bank.sabbirhosenjoy.com`, and the required Render CORS allowlist.
