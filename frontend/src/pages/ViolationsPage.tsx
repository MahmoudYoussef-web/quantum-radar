import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Page, ViolationSummary } from '../api/types'
import { Empty, ErrorBox, Loading, Pager } from '../components/Status'

const PAGE_SIZE = 10

interface Filters {
  plate: string
  rule: string
  device: string
  from: string
  to: string
  minFee: string
  maxFee: string
}

const BLANK: Filters = { plate: '', rule: '', device: '', from: '', to: '', minFee: '', maxFee: '' }

function fmtTime(iso: string): string {
  const d = new Date(iso)
  return d.toLocaleString('en-GB', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function ViolationsPage() {
  const [draft, setDraft] = useState<Filters>(BLANK)
  const [applied, setApplied] = useState<Filters>(BLANK)
  const [page, setPage] = useState(0)

  const set = (k: keyof Filters) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setDraft({ ...draft, [k]: e.target.value })

  const query = useQuery({
    queryKey: ['violations', applied, page],
    queryFn: () => {
      const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) })
      const asIso = (d: string, end: boolean) => (d ? new Date(`${d}T${end ? '23:59:59' : '00:00:00'}Z`).toISOString() : '')
      const entries: [string, string][] = [
        ['plate', applied.plate.trim()],
        ['rule', applied.rule.trim()],
        ['device', applied.device.trim()],
        ['from', asIso(applied.from, false)],
        ['to', asIso(applied.to, true)],
        ['minFee', applied.minFee.trim()],
        ['maxFee', applied.maxFee.trim()],
      ]
      for (const [k, v] of entries) if (v) params.set(k, v)
      return api<Page<ViolationSummary>>(`/api/v1/violations?${params}`)
    },
  })

  const apply = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    setApplied(draft)
  }

  const clear = () => {
    setDraft(BLANK)
    setApplied(BLANK)
    setPage(0)
  }

  return (
    <div>
      <div className="page-head">
        <h1>Violations</h1>
        <p>
          Every recorded rule hit with its source device and timestamp, newest first —{' '}
          {query.data ? `${query.data.totalElements} total` : 'loading total'}.
        </p>
      </div>
      <form className="filters" onSubmit={apply} aria-label="Filter violations">
        <div className="field">
          <label htmlFor="f-plate">Plate</label>
          <input id="f-plate" placeholder="ABC1234" value={draft.plate} onChange={set('plate')} autoComplete="off" />
        </div>
        <div className="field">
          <label htmlFor="f-rule">Rule</label>
          <input id="f-rule" placeholder="SEATBELT" value={draft.rule} onChange={set('rule')} autoComplete="off" />
        </div>
        <div className="field">
          <label htmlFor="f-device">Device</label>
          <input id="f-device" placeholder="RADAR-001" value={draft.device} onChange={set('device')} autoComplete="off" />
        </div>
        <div className="field">
          <label htmlFor="f-from">From</label>
          <input id="f-from" type="date" value={draft.from} onChange={set('from')} />
        </div>
        <div className="field">
          <label htmlFor="f-to">To</label>
          <input id="f-to" type="date" value={draft.to} onChange={set('to')} />
        </div>
        <div className="field">
          <label htmlFor="f-min">Min fee</label>
          <input id="f-min" inputMode="numeric" placeholder="0" value={draft.minFee} onChange={set('minFee')} />
        </div>
        <div className="field">
          <label htmlFor="f-max">Max fee</label>
          <input id="f-max" inputMode="numeric" placeholder="1000" value={draft.maxFee} onChange={set('maxFee')} />
        </div>
        <button className="btn" type="submit">
          Apply filters
        </button>
        <button className="btn-ghost btn" type="button" onClick={clear}>
          Clear
        </button>
      </form>
      {query.isPending && <Loading what="violations" />}
      {query.isError && <ErrorBox message={(query.error as Error).message} onRetry={() => query.refetch()} />}
      {query.data && query.data.content.length === 0 && <Empty what="violations match these filters" />}
      {query.data && query.data.content.length > 0 && (
        <>
          <div className="table-scroll">
            <table className="grid">
              <thead>
                <tr>
                  <th className="num">ID</th>
                  <th>Time</th>
                  <th>Plate</th>
                  <th>Device</th>
                  <th>Rule</th>
                  <th>Description</th>
                  <th className="num">Fee (EGP)</th>
                  <th className="num">Points</th>
                </tr>
              </thead>
              <tbody>
                {query.data.content.map((v) => (
                  <tr key={v.id}>
                    <td className="num">{v.id}</td>
                    <td className="mono" style={{ whiteSpace: 'nowrap' }}>{fmtTime(v.recordedAt)}</td>
                    <td className="mono">{v.plateNumber}</td>
                    <td className="mono">{v.deviceCode ?? '—'}</td>
                    <td>
                      <span className="pill pill-warn">{v.ruleName}</span>
                    </td>
                    <td>{v.description}</td>
                    <td className="num">{v.fee}</td>
                    <td className="num">{v.points}</td>
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
