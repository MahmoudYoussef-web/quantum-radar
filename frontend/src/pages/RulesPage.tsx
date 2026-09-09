import { Fragment, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { FineTier, RuleConfig, RuleVersion } from '../api/types'
import { Empty, ErrorBox, Loading } from '../components/Status'
import { useToast } from '../components/Toast'

export function RulesPage() {
  const client = useQueryClient()
  const notify = useToast()
  const [editing, setEditing] = useState<string | null>(null)
  const [expanded, setExpanded] = useState<string | null>(null)
  const [fee, setFee] = useState('')

  const query = useQuery({ queryKey: ['rules'], queryFn: () => api<RuleConfig[]>('/api/v1/rules') })
  const tiersQuery = useQuery({
    queryKey: ['fine-tiers'],
    queryFn: () => api<FineTier[]>('/api/v1/fine-tiers'),
  })

  const update = useMutation({
    mutationFn: ({ code, body }: { code: string; body: object }) =>
      api<RuleConfig>(`/api/v1/rules/${code}`, { method: 'PATCH', body: JSON.stringify(body) }),
    onSuccess: (rule, variables) => {
      setEditing(null)
      client.invalidateQueries({ queryKey: ['rules'] })
      const body = variables.body as { enabled?: boolean; fee?: number }
      if (body.enabled !== undefined) {
        notify(`Rule ${rule.code} ${rule.enabled ? 'enabled' : 'disabled'}.`)
      } else if (body.fee !== undefined) {
        notify(`Fee updated for ${rule.code}: ${rule.fee} EGP.`)
      }
    },
  })

  return (
    <div>
      <div className="page-head">
        <h1>Rules</h1>
        <p>
          The rule book. Changes apply to the next observation — no deploy. Disabling a
          rule stops it from matching immediately.
        </p>
      </div>
      {query.isPending && <Loading what="rules" />}
      {query.isError && <ErrorBox message={(query.error as Error).message} onRetry={() => query.refetch()} />}
      {query.data && query.data.length === 0 && <Empty what="rules" />}
      {query.data && query.data.length > 0 && (
        <div className="table-scroll">
          <table className="grid">
            <thead>
              <tr>
                <th>Rule</th>
                <th>Status</th>
                <th className="num">Fee (EGP)</th>
                <th className="num">Points</th>
                <th className="num">Max speed</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {query.data.map((r) => (
                <Fragment key={r.code}>
                  <tr>
                  <td>
                    <div className="mono">{r.code}</div>
                    <div className="muted" style={{ fontSize: '0.82rem' }}>{r.displayName}</div>
                  </td>
                  <td>
                    <span className={r.enabled ? 'pill pill-on' : 'pill pill-off'}>
                      {r.enabled ? 'ENABLED' : 'DISABLED'}
                    </span>
                  </td>
                  <td className="num">
                    {editing === r.code ? (
                      <>
                        <label className="muted" htmlFor={`fee-${r.code}`} style={{ display: 'none' }}>
                          Fee for {r.code}
                        </label>
                        <input
                          id={`fee-${r.code}`}
                          value={fee}
                          onChange={(e) => setFee(e.target.value)}
                          size={6}
                          inputMode="numeric"
                        />
                      </>
                    ) : (
                      r.fee
                    )}
                  </td>
                  <td className="num">{r.penaltyPoints}</td>
                  <td className="num">{r.maxSpeed === null ? '—' : `${r.maxSpeed} km/h`}</td>
                  <td>
                    <span className="actions" style={{ display: 'flex', gap: '0.4rem' }}>
                      <button
                        className="btn-ghost btn btn-sm"
                        onClick={() => setExpanded(expanded === r.code ? null : r.code)}
                        aria-expanded={expanded === r.code}
                      >
                        {expanded === r.code ? 'Hide versions' : 'Versions'}
                      </button>
                      <button
                        className="btn-ghost btn btn-sm"
                        onClick={() => update.mutate({ code: r.code, body: { enabled: !r.enabled } })}
                      >
                        {r.enabled ? 'Disable' : 'Enable'}
                      </button>
                      {editing === r.code ? (
                        <button
                          className="btn btn-sm"
                          onClick={() => update.mutate({ code: r.code, body: { fee: Number(fee) } })}
                        >
                          Save
                        </button>
                      ) : (
                        <button
                          className="btn-ghost btn btn-sm"
                          onClick={() => {
                            setEditing(r.code)
                            setFee(String(r.fee))
                          }}
                        >
                          Edit fee
                        </button>
                      )}
                    </span>
                  </td>
                </tr>
                  {expanded === r.code && (
                    <tr>
                      <td colSpan={6} style={{ background: '#1a150d' }}>
                        <RuleVersions code={r.code} tiers={tiersQuery.data ?? []} />
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {update.isError && <p className="error-text">{(update.error as Error).message}</p>}
    </div>
  )
}

function fmtDateTime(iso: string): string {
  return new Date(iso).toLocaleString('en-GB', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function RuleVersions({ code, tiers }: { code: string; tiers: FineTier[] }) {
  const versionsQuery = useQuery({
    queryKey: ['rule-versions', code],
    queryFn: () => api<RuleVersion[]>(`/api/v1/rules/${code}/versions`),
  })
  const live = tiers.filter((t) => t.ruleCode === code)

  return (
    <div style={{ padding: '0.25rem 0' }}>
      <h3 className="panel-title">
        Versions <span className="muted">— new fines pin the version they were judged under</span>
      </h3>
      {versionsQuery.isPending && <Loading what={`versions of ${code}`} />}
      {versionsQuery.isError && (
        <ErrorBox
          message={(versionsQuery.error as Error).message}
          onRetry={() => versionsQuery.refetch()}
        />
      )}
      {versionsQuery.data && (
        <div className="table-scroll" style={{ boxShadow: 'none' }}>
          <table className="grid">
            <thead>
              <tr>
                <th>Version</th>
                <th>Status</th>
                <th className="num">Fee</th>
                <th className="num">Points</th>
                <th className="num">Max speed</th>
                <th>Effective from</th>
              </tr>
            </thead>
            <tbody>
              {versionsQuery.data.map((v) => (
                <tr key={v.version}>
                  <td className="mono">v{v.version}</td>
                  <td>
                    <span className={v.enabled ? 'pill pill-on' : 'pill pill-off'}>
                      {v.enabled ? 'ON' : 'OFF'}
                    </span>
                  </td>
                  <td className="num">{v.fee}</td>
                  <td className="num">{v.penaltyPoints}</td>
                  <td className="num">{v.maxSpeed === null ? '—' : `${v.maxSpeed} km/h`}</td>
                  <td className="mono">{fmtDateTime(v.effectiveFrom)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <h3 className="panel-title" style={{ marginTop: '1rem' }}>
        Live pricing tiers <span className="muted">— global policy, violations pin their fee at write time</span>
      </h3>
      {live.length === 0 && <p className="muted">No tiers — the base fee applies.</p>}
      {live.length > 0 && (
        <ul style={{ listStyle: 'none', margin: 0, padding: 0, lineHeight: 2 }} className="mono">
          {live
            .slice()
            .sort((a, b) => a.overFrom - b.overFrom)
            .map((t) => (
              <li key={t.id}>
                {t.overFrom}–{t.overTo === null ? '∞' : t.overTo} km/h over → {t.fee} EGP
              </li>
            ))}
        </ul>
      )}
    </div>
  )
}
