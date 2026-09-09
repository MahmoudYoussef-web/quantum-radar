export interface DayPoint {
  date: string
  count: number
}

export interface RulePoint {
  rule: string
  count: number
}

/** Hand-rolled SVG charts: zero dependencies, honest scales, no decoration. */

export function TrendChart({ points, days = 14 }: { points: DayPoint[]; days?: number }) {
  const W = 560
  const H = 220
  const PAD_L = 36
  const PAD_B = 26
  const PAD_T = 12
  const PAD_R = 8

  const byDate = new Map(points.map((p) => [p.date, p.count]))
  const today = new Date()
  const series: { date: string; count: number }[] = []
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    const key = d.toISOString().slice(0, 10)
    series.push({ date: key, count: byDate.get(key) ?? 0 })
  }

  const max = Math.max(1, ...series.map((s) => s.count))
  const niceMax = Math.max(1, Math.ceil(max / 5) * 5)
  const x = (i: number) => PAD_L + (i * (W - PAD_L - PAD_R)) / Math.max(1, series.length - 1)
  const y = (v: number) => PAD_T + (H - PAD_T - PAD_B) * (1 - v / niceMax)
  const line = series.map((s, i) => `${i === 0 ? 'M' : 'L'}${x(i).toFixed(1)},${y(s.count).toFixed(1)}`).join(' ')
  const area = `${line} L${x(series.length - 1).toFixed(1)},${y(0).toFixed(1)} L${x(0).toFixed(1)},${y(0).toFixed(1)} Z`
  const ticks = [0, Math.ceil(niceMax / 2), niceMax]
  const total = series.reduce((a, s) => a + s.count, 0)

  return (
    <figure className="chart" style={{ margin: 0 }}>
      <svg
        viewBox={`0 0 ${W} ${H}`}
        role="img"
        aria-label={`Violations per day for the last ${days} days, ${total} total`}
      >
        {ticks.map((t) => (
          <g key={t}>
            <line x1={PAD_L} x2={W - PAD_R} y1={y(t)} y2={y(t)} className="gridline" />
            <text x={PAD_L - 6} y={y(t) + 4} textAnchor="end" className="tick">
              {t}
            </text>
          </g>
        ))}
        <path d={area} className="area" />
        <path d={line} className="line" fill="none" />
        {series.map((s, i) =>
          s.count > 0 ? <circle key={s.date} cx={x(i)} cy={y(s.count)} r="3" className="dot" /> : null,
        )}
        <text x={PAD_L} y={H - 6} className="tick">
          {series[0].date.slice(5)}
        </text>
        <text x={W - PAD_R} y={H - 6} textAnchor="end" className="tick">
          {series[series.length - 1].date.slice(5)}
        </text>
      </svg>
      <figcaption className="muted">
        {total} violations in the last {days} days
      </figcaption>
    </figure>
  )
}

export function RuleBars({ rows }: { rows: RulePoint[] }) {
  const sorted = [...rows].sort((a, b) => b.count - a.count)
  const max = Math.max(1, ...sorted.map((r) => r.count))
  const total = sorted.reduce((a, r) => a + r.count, 0)
  return (
    <div
      role="img"
      aria-label={`Violations by rule: ${sorted.map((r) => `${r.rule} ${r.count}`).join(', ') || 'none'}`}
    >
      {sorted.length === 0 && <p className="muted">No violations recorded.</p>}
      {sorted.map((r) => (
        <div className="bar-row" key={r.rule}>
          <span className="mono bar-label">{r.rule}</span>
          <span className="bar-track">
            <span className="bar-fill" style={{ width: `${(100 * r.count) / max}%` }} />
          </span>
          <span className="mono bar-count">{r.count}</span>
        </div>
      ))}
      {total > 0 && (
        <p className="muted" style={{ marginBottom: 0 }}>
          {total} total
        </p>
      )}
    </div>
  )
}
