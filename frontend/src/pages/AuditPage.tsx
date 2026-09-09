import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Page } from '../api/types'
import { Empty, ErrorBox, Loading, Pager } from '../components/Status'
import { useState } from 'react'

export interface AuditEntry {
  id: number
  occurredAt: string
  actor: string
  action: string
  entityType: string | null
  entityId: string | null
  details: string | null
  ipAddress: string | null
}

function fmtTime(iso: string): string {
  return new Date(iso).toLocaleString('en-GB', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function AuditPage() {
  const [page, setPage] = useState(0)

  const query = useQuery({
    queryKey: ['audit', page],
    queryFn: () => api<Page<AuditEntry>>(`/api/v1/audit?page=${page}&size=15`),
  })

  return (
    <div>
      <div className="page-head">
        <h1>Audit log</h1>
        <p>Who changed what, from which value to which, and from where. Newest first.</p>
      </div>
      {query.isPending && <Loading what="audit entries" />}
      {query.isError && <ErrorBox message={(query.error as Error).message} onRetry={() => query.refetch()} />}
      {query.data && query.data.content.length === 0 && <Empty what="audit entries" />}
      {query.data && query.data.content.length > 0 && (
        <>
          <div className="table-scroll">
            <table className="grid">
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Actor</th>
                  <th>Action</th>
                  <th>Entity</th>
                  <th>Details</th>
                  <th>IP</th>
                </tr>
              </thead>
              <tbody>
                {query.data.content.map((a) => (
                  <tr key={a.id}>
                    <td className="mono" style={{ whiteSpace: 'nowrap' }}>{fmtTime(a.occurredAt)}</td>
                    <td>{a.actor}</td>
                    <td>
                      <span className="pill pill-warn">{a.action}</span>
                    </td>
                    <td className="mono">
                      {a.entityType ?? '—'} {a.entityId ?? ''}
                    </td>
                    <td className="mono" style={{ fontSize: '0.8rem', maxWidth: '280px', overflow: 'hidden', textOverflow: 'ellipsis' }} title={a.details ?? ''}>
                      {a.details ?? '—'}
                    </td>
                    <td className="mono">{a.ipAddress ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pager page={query.data.number} totalPages={query.data.totalPages} onPage={setPage} />
        </>
      )}
    </div>
  )
}
