# QuRadar Admin

Small admin UI for the [Quantum Radar](../quantum-radar) backend — violations table
(filter/sort/paginate), rule management (enable, edit fees), device list, and
driver/vehicle lookup with violation history. Loading, empty, and error states
throughout. No maps, no real-time push, no charts — those are documented v3 ideas
in the backend README, not half-built features here.

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
# from the backend repo (this folder must sit next to it):
cd ../quantum-radar
docker compose up --build
# backend :8080, admin UI :5173
```

## Stack

Vite + React 18 + TypeScript + TanStack Query + plain `fetch` (no axios).
Built image serves `dist/` from nginx (see `Dockerfile`, `nginx.conf`).
