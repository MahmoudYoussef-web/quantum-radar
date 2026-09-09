# QuRadar Admin

Small admin UI for the [Quantum Radar](../backend) backend — public landing page,
shift overview (KPIs, hand-rolled SVG trend + distribution charts), violations
table (filters, pagination, timestamps, device source), rule management (enable,
fees, version history, live tiers), device registry with health and details,
driver lookup with enforcement summaries, and an audit log. Loading, empty, and
error states plus mutation toasts throughout. No maps, no real-time push — those
stay documented v3 ideas in the backend README.

## Run

```bash
cp .env.example .env   # VITE_API_URL=http://localhost:8080
npm install
npm run dev            # http://localhost:5173
```

Login with the backend bootstrap admin (`admin` / `ADMIN_PASSWORD`, default
`admin123`). Tokens live in `localStorage`; the API client silently refreshes
once on 401 and otherwise sends you back to `/login`.

## Full stack

```bash
# full stack from the monorepo backend dir:
cd ../backend
docker compose up --build
# backend :8080, admin UI :5173
```

## Stack

Vite + React 18 + TypeScript + TanStack Query + plain `fetch` (no axios).
Built image serves `dist/` from nginx (see `Dockerfile`, `nginx.conf`).
