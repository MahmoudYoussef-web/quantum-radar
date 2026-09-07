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
      <h2>Rules</h2>
      {query.isPending && <Loading what="rules" />}
      {query.isError && <ErrorBox message={(query.error as Error).message} onRetry={() => query.refetch()} />}
      {query.data && query.data.length === 0 && <Empty what="rules" />}
      {query.data && query.data.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Code</th>
              <th>Name</th>
              <th>Enabled</th>
              <th>Fee</th>
              <th>Points</th>
              <th>Max speed</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {query.data.map((r) => (
              <tr key={r.code}>
                <td>{r.code}</td>
                <td>{r.displayName}</td>
                <td>{r.enabled ? 'yes' : 'no'}</td>
                <td>
                  {editing === r.code ? (
                    <input value={fee} onChange={(e) => setFee(e.target.value)} size={6} />
                  ) : (
                    r.fee
                  )}
                </td>
                <td>{r.penaltyPoints}</td>
                <td>{r.maxSpeed ?? '—'}</td>
                <td className="actions">
                  <button
                    onClick={() =>
                      update.mutate({ code: r.code, body: { enabled: !r.enabled } })
                    }
                  >
                    {r.enabled ? 'Disable' : 'Enable'}
                  </button>
                  {editing === r.code ? (
                    <button
                      onClick={() => update.mutate({ code: r.code, body: { fee: Number(fee) } })}
                    >
                      Save
                    </button>
                  ) : (
                    <button
                      onClick={() => {
                        setEditing(r.code)
                        setFee(String(r.fee))
                      }}
                    >
                      Edit fee
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {update.isError && <p className="error-text">{(update.error as Error).message}</p>}
    </div>
  )
}
