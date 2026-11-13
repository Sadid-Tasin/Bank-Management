# Confused Bank — Frontend

Next.js App Router frontend for the Confused Bank Spring Boot banking API.

## Run the frontend locally

From the repository root:

```bash
cd frontend
npm ci
npm run dev
```

Open `http://localhost:3000` in your browser. The frontend uses the configured Render API by default, so no `.env.local` file is required for the default setup.

To point the frontend at a locally running Spring Boot API instead:

```bash
cd frontend
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080 npm run dev
```

## Run the backend locally (optional)

The backend's `local` profile reads database credentials and the JWT secret from environment variables. Set them in your own terminal session; do not add real credentials to source files:

```bash
cd backend
export SPRING_PROFILES_ACTIVE=local
export DB_URL='jdbc:postgresql://YOUR_DATABASE_HOST:5432/YOUR_DATABASE?sslmode=require'
export DB_USERNAME='YOUR_DATABASE_USERNAME'
export DB_PASSWORD='YOUR_DATABASE_PASSWORD'
export APP_JWT_SECRET='YOUR_LONG_RANDOM_BASE64_SECRET'
export APP_CORS_ALLOWED_ORIGINS='http://localhost:3000'
./mvnw spring-boot:run
```

Run the frontend in a second terminal with `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080 npm run dev`.

## Production checks

```bash
npm run typecheck
npm run lint
npm run build
```

The frontend keeps the existing backend authentication contract, JWT session behavior, customer registration endpoint, role-specific navigation, and API-backed dashboard modules.
