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
      <h2>Violations</h2>
      <form className="filters" onSubmit={apply}>
        <input placeholder="Plate (e.g. ABC1234)" value={plate} onChange={(e) => setPlate(e.target.value)} />
        <input placeholder="Rule (e.g. SEATBELT)" value={rule} onChange={(e) => setRule(e.target.value)} />
        <button type="submit">Filter</button>
      </form>
      {query.isPending && <Loading what="violations" />}
      {query.isError && <ErrorBox message={(query.error as Error).message} onRetry={() => query.refetch()} />}
      {query.data && query.data.content.length === 0 && <Empty what="violations" />}
      {query.data && query.data.content.length > 0 && (
        <>
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Plate</th>
                <th>Rule</th>
                <th>Description</th>
                <th>Fee</th>
                <th>Points</th>
              </tr>
            </thead>
            <tbody>
              {query.data.content.map((v) => (
                <tr key={v.id}>
                  <td>{v.id}</td>
                  <td>{v.plateNumber}</td>
                  <td>{v.ruleName}</td>
                  <td>{v.description}</td>
                  <td>{v.fee}</td>
                  <td>{v.points}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <Pager page={query.data.number} totalPages={query.data.totalPages} onPage={setPage} />
        </>
      )}
    </div>
  )
}
