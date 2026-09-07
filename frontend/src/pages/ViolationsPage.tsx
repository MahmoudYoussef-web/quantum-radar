import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Page, ViolationSummary } from '../api/types'
import { Empty, ErrorBox, Loading, Pager } from '../components/Status'

const PAGE_SIZE = 10

export function ViolationsPage() {
  const [plate, setPlate] = useState('')
  const [rule, setRule] = useState('')
  const [page, setPage] = useState(0)
  const [applied, setApplied] = useState({ plate: '', rule: '' })

  const query = useQuery({
    queryKey: ['violations', applied, page],
    queryFn: () => {
      const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) })
      if (applied.plate) params.set('plate', applied.plate)
      if (applied.rule) params.set('rule', applied.rule)
      return api<Page<ViolationSummary>>(`/api/v1/violations?${params}`)
    },
  })

  const apply = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    setApplied({ plate: plate.trim(), rule: rule.trim() })
  }

  return (
    <div>
      <div className="page-head">
        <h1>Violations</h1>
        <p>
          Every recorded rule hit, newest first. Filter by plate or rule code —{' '}
          {query.data ? `${query.data.totalElements} total` : 'loading total'}.
        </p>
      </div>
      <form className="filters" onSubmit={apply} aria-label="Filter violations">
        <div className="field">
          <label htmlFor="f-plate">Plate</label>
          <input
            id="f-plate"
            placeholder="ABC1234"
            value={plate}
            onChange={(e) => setPlate(e.target.value)}
            autoComplete="off"
          />
        </div>
        <div className="field">
          <label htmlFor="f-rule">Rule</label>
          <input
            id="f-rule"
            placeholder="SEATBELT"
            value={rule}
            onChange={(e) => setRule(e.target.value)}
            autoComplete="off"
          />
        </div>
        <button className="btn" type="submit">
          Filter
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
                  <th>Plate</th>
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
                    <td className="mono">{v.plateNumber}</td>
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
