# Quantum Radar (QuRadar)

Traffic radar backend: radar devices submit `Observation`s, a DB-configurable rules
engine evaluates them, and the service persists violations + tiered fines + driver
penalty points. Single Spring Boot modular monolith — no microservices.

```bash
# one-command local run (needs the admin repo next to this one for the UI)
docker compose up --build
# backend :8080 · admin UI :5173 · postgres :5432 · redis :6379
```

Local dev without Docker: `mvn spring-boot:run` (needs Postgres + Redis; override
with `SPRING_DATASOURCE_URL`, `REDIS_HOST`/`REDIS_PORT` — e.g. ports `5433`/`6380`
if yours are taken). Bootstrap admin: `admin` / `ADMIN_PASSWORD` (default
`admin123`, warned at startup). API docs: `/swagger-ui.html`.

## Architecture

```mermaid
flowchart LR
    device[Radar device] -- "POST /api/v1/events (eventId)" --> api[EventsController]
    api --> radar[QuRadar: validate device → run rules → price → persist]
    radar --> rules[(rule_configs)]
    radar --> tiers[(fine_tiers)]
    radar --> pg[(Postgres: observations/fines/violations/drivers/...)]
    radar <--> redis[(Redis: idempotency fast-path + rate limits)]
    ui[Admin UI (separate repo)] -- JWT REST --> api
    api --> admin[Admin controllers: rules/devices/drivers/vehicles/tiers/users]
```

Packages: `ingestion/` (intake API) · `rules/` (engine + config) ·
`violation/` · `fine/` (calc + queries) · `vehicle/` · `driver/` ·
`device/` (registry) · `security/` (JWT/roles) · `common/` · `config/`.

## Data model

```mermaid
erDiagram
    observations ||--o{ fines : "produce"
    fines ||--|{ violations : "contain"
    observations }o--o| devices : "reported by"
    observations ||--|| rule_configs : "evaluated by"
    vehicles }o--o| drivers : "owned by"
    drivers ||--|| licenses : "holds"
    drivers ||--o{ vehicles : "owns"
    users }o--o| drivers : "login as"
    users }o--o| devices : "login as"
    users ||--o{ refresh_tokens : "sessions"
    fine_tiers }o--|| rule_configs : "price"
```

Migrations are strictly forward-only (`V1` observations/fines/violations →
`V2` rule configs → `V3` signals → `V4` devices+eventId → `V5`
driver/license/vehicle/tiers → `V6` users/tokens). Applied files are frozen.

## API reference

| Method | Path | Role | Notes |
|---|---|---|---|
| POST | `/api/v1/auth/login` | public | → access (15m) + refresh (7d) JWT |
| POST | `/api/v1/auth/refresh` | public | rotates: old token revoked |
| POST | `/api/v1/auth/logout` | public | revokes refresh token, always 204 |
| POST | `/api/v1/events` | DEVICE (own) / ADMIN | 201 fine, 204 clean, 409 replay, 404 device |
| GET/PATCH | `/api/v1/rules[/{code}]` | ADMIN | enable, edit fee/points/maxSpeed |
| GET/POST/PATCH | `/api/v1/devices[/{code}]` | ADMIN | registry |
| POST / GET | `/api/v1/drivers` | ADMIN / ADMIN+OFFICER+own CITIZEN | lookup by licenseNo |
| POST / GET | `/api/v1/vehicles` | ADMIN / +history scoped | register, history with fines |
| GET | `/api/v1/fines?plate=&page=&size=` | ADMIN/OFFICER/own CITIZEN | paginated |
| GET | `/api/v1/violations?rule=&plate=&page=&size=` | ADMIN/OFFICER/own CITIZEN | paginated |
| GET/POST/DELETE | `/api/v1/fine-tiers` | ADMIN | tiered pricing |
| GET/POST | `/api/v1/admin/users` | ADMIN | create logins (CITIZEN↔driver, DEVICE↔device) |

Auth: BCrypt passwords, JWT access + rotating refresh (SHA-256 hashes stored
server-side), stateless filter, `@PreAuthorize` + `SecuritySupport` ownership
checks. Ownership is resolved from the JWT identity against the DB — client
fields are never trusted (DEVICE posting another device's code → 403,
CITIZEN reading another plate → 403).

## Trade-offs you can defend

**Idempotency without Kafka.** `observations.event_id` has a DB UNIQUE NOT NULL
constraint: a replay fails closed with 409 instead of double-fining. An outbox +
broker would add throughput and async retries at the cost of a broker, consumer
lag, and exactly-once plumbing nobody here needs yet. Redis (`idem:event:*`,
24h TTL, written after commit) is only a fast-path 409; on misses or outages
the constraint decides — proven live by replaying with the DB row deleted.

**Concurrency without distributed locks.** `@Version` on `Driver` (points) and
`FineEntity` (totals): two violations for one driver racing → one commits, the
loser gets `OptimisticLockingFailureException` and retries. Correct because
contention is rare; a pessimistic lock or queue would serialize all ingestion
for no benefit. Covered by `OptimisticLockingIT`.

**Sync processing.** Validate → rules → fine → persist in one transaction keeps
the demo truthful and the failure modes obvious (409/422/5xx right in the
response). It caps throughput — that is an explicit v3 problem (async pipeline).

**Rules in the DB.** Thresholds/fees/points/enabled are rows, not code: ADMIN
edits apply on the next observation, no redeploy. New rule = new `ViolationRule`
POJO + `RuleConfigService.toRule` case + seed row + unit test (each rule has
zero Spring imports, testable alone).

## Testing

- `mvn test` — 27 unit/slice tests (every rule, tier math, builder filtering, controller slice).
- `mvn verify` — Testcontainers suites (`*IT`, failsafe): ingestion 201/409/404/400/401,
  auth rotation/logout, optimistic-locking conflict, Redis cache-served 409. Needs Docker;
  on Windows + Docker Desktop 29 set `DOCKER_HOST=npipe:////./pipe/dockerDesktopLinuxEngine`.
- CI (`.github/workflows/ci.yml`) runs `mvn -B verify` + a Docker build check on every push/PR.

## Configuration

| Var | Default | Purpose |
|---|---|---|
| `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` | `localhost:5432/quradar` | Postgres |
| `REDIS_HOST/PORT` | `localhost:6379` | Redis (cache only) |
| `JWT_SECRET` | dev default | HS256 key (≥32 bytes), rotate in prod |
| `ADMIN_USERNAME/PASSWORD` | `admin/admin123` | bootstrap admin if users empty |
| `RATELIMIT_EVENTS_PER_MIN/AUTH_PER_MIN` | `60/10` | Redis fixed-window limits, fail-open |

`DemoRunner` replays the original 4-observation scenario on boot (fresh UUID
eventIds, `quradar.demo.enabled=false` in tests); REST ingestion is the real path.

## Future work (v3, deliberate — not started)

Kafka + transactional outbox for async ingestion · PostGIS zone queries ·
OpenTelemetry/Prometheus/Grafana · WebSocket/SSE live violation feed · charts and
map views in the admin UI · per-driver fine statements/PDF.
