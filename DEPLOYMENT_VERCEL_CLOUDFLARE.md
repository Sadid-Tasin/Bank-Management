# Confused Bank — GitHub, Vercel and Cloudflare Deployment

Assumed production hostname: `bank.sabbirhosenjoy.com`.

## Repository layout

This repository contains the Spring Boot backend at `backend/` and the Next.js frontend at `frontend/`. Vercel must deploy only `frontend/`.

## Before pushing

From the existing Git repository at `~/Documents/Bank-Management-System`, copy the `frontend/` directory from this release into the repository. Preserve `frontend/.env.local`, `frontend/node_modules/`, and `frontend/.next/` locally; never commit `.env.local`.

Run from `frontend/`:

```bash
npm ci
npm run typecheck
npm run lint
npm run build
```

## Vercel project configuration

- Git repository: `Sabbir086/bank-management-system`
- Production branch: `main`
- Root Directory: `frontend`
- Framework: Next.js
- Install Command: `npm ci` (or Vercel's detected default)
- Build Command: `npm run build`
- Output Directory: `.next` (default for Next.js)
- Environment Variable (Production, and Preview if preview deployments are used):
  - `NEXT_PUBLIC_API_BASE_URL=https://bank-management-system-m3th.onrender.com`

Do not add Supabase database credentials to Vercel. The browser app calls the Spring Boot API; the backend owns database access.

## Custom subdomain

1. In the Vercel project, open Settings → Domains and add `bank.sabbirhosenjoy.com`.
2. Copy the exact CNAME target Vercel displays for this domain. Do not assume a generic target if Vercel supplies a project-specific target.
3. In Cloudflare → `sabbirhosenjoy.com` → DNS, create/update:
   - Type: `CNAME`
   - Name: `bank`
   - Target: the exact Vercel CNAME target
   - TTL: Auto
   - Proxy status: DNS only (gray cloud) for the initial Vercel domain verification and TLS setup.
4. Do not change the apex/root (`@`) records, mail records, or nameservers for this subdomain setup.
5. Wait for Vercel to show the domain as configured and for its TLS certificate to be issued.

## Backend CORS

In Render → `bank-management-system` → Environment, set `APP_CORS_ALLOWED_ORIGINS` to a comma-separated allowlist with no trailing slashes. Example (replace the Vercel project URL with the real stable `.vercel.app` URL shown by Vercel):

```text
http://localhost:3000,https://YOUR-PROJECT.vercel.app,https://bank.sabbirhosenjoy.com
```

Preserve any other trusted origin already in the value. Save and redeploy/restart the Render backend. Do not use `*`.

## Git commit and push

Stage only the frontend and deployment documentation unless other changes were deliberately made:

```bash
git add frontend README_FRONTEND_INTEGRATION.md DEPLOYMENT_VERCEL_CLOUDFLARE.md
git diff --cached --check
git commit -m "feat: deploy Confused Bank frontend"
git push origin main
```

Once the GitHub repository is connected to Vercel, pushes to the configured production branch trigger new production deployments.

## Final checks

- Visit `https://bank.sabbirhosenjoy.com`.
- Sign in with the existing Admin account.
- Confirm Dashboard and profile load without a browser CORS error.
- Test one safe, read-only endpoint first (profile, overview, list). Do not test deposit/withdraw/transfer with real balances until the workflow has been deliberately reviewed.
