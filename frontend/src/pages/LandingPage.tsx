import { Link } from 'react-router-dom'

export function RadarMark({ size = 26 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 26 26" aria-hidden="true" focusable="false">
      <circle cx="13" cy="13" r="11.5" fill="none" stroke="currentColor" strokeWidth="1.6" />
      <circle cx="13" cy="13" r="6.5" fill="none" stroke="currentColor" strokeWidth="1.2" opacity="0.6" />
      <line x1="13" y1="13" x2="13" y2="1.5" stroke="currentColor" strokeWidth="1.6" />
      <circle cx="13" cy="13" r="2" fill="currentColor" />
      <circle cx="18.5" cy="9" r="1.6" fill="currentColor" />
    </svg>
  )
}

const RULES: [string, string, string][] = [
  ['SEATBELT', 'Belt not fastened', '100 EGP · 1 pt'],
  ['SPEED_LIMIT_PRIVATE', 'Private cars over 80 km/h', 'tiered from 300 EGP · 2 pts'],
  ['SPEED_LIMIT_TRUCK', 'Trucks over 60 km/h', 'tiered from 300 EGP · 2 pts'],
  ['RESTRICTED_ZONE', 'Inside the bounded zone', '500 EGP · 2 pts'],
  ['RED_LIGHT', 'Stop line crossed on red', '500 EGP · 3 pts'],
]

const STEPS: [string, string][] = [
  ['01 — Ingest', 'A device posts an observation with its own eventId. Unknown devices are rejected; replays get a 409.'],
  ['02 — Judge', 'Enabled rules are built from the database on every call — no redeploys, no hardcoded thresholds.'],
  ['03 — Price', 'Speeding fees come from over-limit tiers; other rules fall back to their configured base fee.'],
  ['04 — Attribute', 'Penalty points land on the plate owner in the same transaction, guarded by optimistic locking.'],
]

export function LandingPage() {
  return (
    <div>
      <a className="skip" href="#main">
        Skip to content
      </a>
      <header className="l-nav">
        <div className="wrap">
          <Link className="brand" to="/" aria-label="QuRadar home">
            <RadarMark /> QuRadar
          </Link>
          <nav className="l-links" aria-label="Product">
            <a href="#how">How it works</a>
            <a href="#rules">Rules</a>
            <a href="#api">API</a>
          </nav>
          <Link className="btn btn-sm" to="/login">
            Open the console
          </Link>
        </div>
      </header>

      <main id="main">
        <section className="hero">
          <div className="wrap hero-grid">
            <div>
              <span className="eyebrow">Traffic enforcement backend</span>
              <h1>Every observation judged. Every fine accounted for.</h1>
              <p className="lede">
                QuRadar takes radar observations, runs them against versioned traffic rules,
                and produces priced, attributable fines — with duplicate replays rejected
                and every decision stored in Postgres.
              </p>
              <div className="hero-cta">
                <Link className="btn" to="/login">
                  Open the console
                </Link>
                <a className="btn btn-ghost" href="#how">
                  How it works
                </a>
              </div>
            </div>
            <div className="console" role="img" aria-label="Illustration of a radar console processing events into fines">
              <div className="console-bar" aria-hidden="true">
                <i />
                <i />
                <i />
                <span>radar-001 · live</span>
              </div>
              <div className="console-body">
                <div className="scope" aria-hidden="true" />
                <ul className="feed" aria-hidden="true">
                  <li><span className="dim">evt-9f31</span> PRIVATE 94 km/h → <span className="fine">SPEED_LIMIT_PRIVATE · 600</span></li>
                  <li><span className="dim">evt-9f32</span> TRUCK 58 km/h → <span className="ok">clean</span></li>
                  <li><span className="dim">evt-9f31</span> replay → <span className="fine">409 duplicate</span></li>
                  <li><span className="dim">evt-9f33</span> no belt → <span className="fine">SEATBELT · 100</span></li>
                  <li><span className="dim">evt-9f34</span> red light → <span className="fine">RED_LIGHT · 500</span></li>
                </ul>
              </div>
            </div>
          </div>
        </section>

        <section className="stats" aria-label="Product facts">
          <div className="wrap">
            <div className="stat"><b>5</b><span>violation rules in the engine</span></div>
            <div className="stat"><b>409</b><span>returned on event replay</span></div>
            <div className="stat"><b>15 min</b><span>access token lifetime</span></div>
            <div className="stat"><b>24 h</b><span>idempotency cache window</span></div>
          </div>
        </section>

        <section className="section" id="how">
          <div className="wrap">
            <h2>One transaction per observation. Nothing half-written.</h2>
            <p className="sub">
              Validate the device, run the rules, price the fine, store everything —
              or store nothing. The database constraint, not application memory, is what
              makes a replay safe.
            </p>
            <ol className="steps">
              {STEPS.map(([title, body]) => (
                <li key={title}>
                  <span className="n">{title.split(' — ')[0]}</span>
                  <h3>{title.split(' — ')[1]}</h3>
                  <p>{body}</p>
                </li>
              ))}
            </ol>
          </div>
        </section>

        <section className="section" id="rules" style={{ paddingTop: 0 }}>
          <div className="wrap">
            <h2>The rule book, editable without a deploy.</h2>
            <p className="sub">
              Thresholds, fees, penalty points and on/off switches live in the database.
              Admins change them from the console; the engine picks them up on the next
              observation.
            </p>
            <div className="table-scroll" style={{ marginTop: '1.75rem' }}>
              <table className="grid">
                <thead>
                  <tr>
                    <th>Rule</th>
                    <th>Triggers when</th>
                    <th className="num">Price</th>
                  </tr>
                </thead>
                <tbody>
                  {RULES.map(([code, when, price]) => (
                    <tr key={code}>
                      <td className="mono">{code}</td>
                      <td>{when}</td>
                      <td className="num">{price}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </section>

        <section className="section" id="api" style={{ paddingTop: 0 }}>
          <div className="wrap">
            <h2>REST first. Authenticated by role.</h2>
            <p className="sub">
              Devices submit for themselves, citizens read their own records, officers
              read operations, admins manage everything — enforced server-side from the
              JWT identity, never from request fields.
            </p>
            <pre className="codeblock" aria-label="Example API calls">
              <code>
                <span className="c"># ingest an observation</span>{'\n'}
                POST /api/v1/events <span className="k">→ 201</span> {'{'}plate, total, violations{'}'}{'\n'}
                <span className="c"># send the same eventId again</span>{'\n'}
                POST /api/v1/events <span className="k">→ 409</span> {'{'}error: duplicate{'}'}
              </code>
            </pre>
          </div>
        </section>
      </main>

      <footer className="l-footer">
        <div className="wrap">
          <span>
            <strong>QuRadar</strong> — radar observations in, attributable fines out.
          </span>
          <span className="mono">postgres · redis · spring boot 3</span>
        </div>
      </footer>
    </div>
  )
}
