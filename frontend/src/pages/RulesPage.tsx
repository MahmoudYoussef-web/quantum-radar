import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { RuleConfig } from '../api/types'
import { Empty, ErrorBox, Loading } from '../components/Status'

export function RulesPage() {
  const client = useQueryClient()
  const [editing, setEditing] = useState<string | null>(null)
  const [fee, setFee] = useState('')

  const query = useQuery({ queryKey: ['rules'], queryFn: () => api<RuleConfig[]>('/api/v1/rules') })

  const update = useMutation({
    mutationFn: ({ code, body }: { code: string; body: object }) =>
      api<RuleConfig>(`/api/v1/rules/${code}`, { method: 'PATCH', body: JSON.stringify(body) }),
    onSuccess: () => {
      setEditing(null)
      client.invalidateQueries({ queryKey: ['rules'] })
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
                <tr key={r.code}>
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
              ))}
            </tbody>
          </table>
        </div>
      )}
      {update.isError && <p className="error-text">{(update.error as Error).message}</p>}
    </div>
  )
}
