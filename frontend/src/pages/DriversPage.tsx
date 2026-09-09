import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { DriverSummary, Page, VehicleHistory } from '../api/types'
import { Empty, ErrorBox, Loading, Pager } from '../components/Status'

interface DriverRow {
  name: string
  licenseNo: string
  penaltyPoints: number
  version: number
  licenseStatus: string | null
}

const PAGE_SIZE = 10

export function DriversPage() {
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<string | null>(null)

  const directory = useQuery({
    queryKey: ['drivers', page],
    queryFn: () =>
      api<Page<DriverRow>>(`/api/v1/drivers?page=${page}&size=${PAGE_SIZE}`),
  })

  return (
    <div>
      <div className="page-head">
        <h1>Drivers &amp; vehicles</h1>
        <p>Every registered driver. Select one for identity, vehicles and enforcement totals.</p>
      </div>

      {directory.isPending && <Loading what="drivers" />}
      {directory.isError && (
        <ErrorBox message={(directory.error as Error).message} onRetry={() => directory.refetch()} />
      )}
      {directory.data && directory.data.content.length === 0 && <Empty what="drivers" />}
      {directory.data && directory.data.content.length > 0 && (
        <>
          <div className="table-scroll">
            <table className="grid">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>License</th>
                  <th>Status</th>
                  <th className="num">Points</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {directory.data.content.map((d) => (
                  <tr
                    key={d.licenseNo}
                    className={selected === d.licenseNo ? 'selected' : undefined}
                  >
                    <td>{d.name}</td>
                    <td className="mono">{d.licenseNo}</td>
                    <td>
                      <span className={d.licenseStatus === 'ACTIVE' ? 'pill pill-on' : 'pill pill-bad'}>
                        {d.licenseStatus ?? 'NO LICENSE'}
                      </span>
                    </td>
                    <td className="num">{d.penaltyPoints}</td>
                    <td>
                      <button
                        className="btn-ghost btn btn-sm"
                        onClick={() => setSelected(selected === d.licenseNo ? null : d.licenseNo)}
                        aria-expanded={selected === d.licenseNo}
                      >
                        {selected === d.licenseNo ? 'Hide' : 'Open'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pager page={directory.data.number} totalPages={directory.data.totalPages} onPage={setPage} />
        </>
      )}

      {selected && <DriverCard licenseNo={selected} onClose={() => setSelected(null)} />}
    </div>
  )
}

function DriverCard({ licenseNo, onClose }: { licenseNo: string; onClose: () => void }) {
  const summaryQuery = useQuery({
    queryKey: ['driver-summary', licenseNo],
    queryFn: () => api<DriverSummary>(`/api/v1/drivers/${licenseNo}/summary`),
    retry: false,
  })

  return (
    <section className="card" aria-label={`Driver ${licenseNo}`} style={{ marginTop: '1rem' }}>
      <h2 className="panel-title">
        Driver file{' '}
        <button className="btn-ghost btn btn-sm" onClick={onClose}>
          Close
        </button>
      </h2>
      {summaryQuery.isPending && <Loading what="driver file" />}
      {summaryQuery.isError && (
        <ErrorBox message={(summaryQuery.error as Error).message} onRetry={() => summaryQuery.refetch()} />
      )}
      {summaryQuery.data && <DriverDetail summary={summaryQuery.data} />}
    </section>
  )
}

function DriverDetail({ summary }: { summary: DriverSummary }) {
  const { driver } = summary
  const [plate, setPlate] = useState<string | null>(null)

  return (
    <div>
      <p style={{ fontSize: '1.25rem', margin: '0 0 0.25rem' }}>
        <strong>{driver.name}</strong>{' '}
        <span className="mono muted">{driver.licenseNo}</span>
      </p>
      <p>
        <span className={driver.licenseStatus === 'ACTIVE' ? 'pill pill-on' : 'pill pill-bad'}>
          {driver.licenseStatus ?? 'NO LICENSE'}
        </span>{' '}
        <span className="mono">{driver.penaltyPoints} pts</span>{' '}
        <span className="muted">record v{driver.version}</span>
      </p>
      <div className="kpis" style={{ margin: '1rem 0 0' }}>
        <div className="kpi">
          <span>Total violations</span>
          <b>{summary.totalViolations}</b>
        </div>
        <div className="kpi">
          <span>Distinct fines</span>
          <b>{summary.totalFines}</b>
        </div>
        <div className="kpi">
          <span>Total fined</span>
          <b>{summary.totalFineAmount.toLocaleString('en-EG')}</b>
          <small>EGP</small>
        </div>
      </div>

      <h3 className="panel-title" style={{ marginTop: '1.25rem' }}>
        Vehicles ({summary.vehicles.length})
      </h3>
      {summary.vehicles.length === 0 && <Empty what="vehicles for this driver" />}
      {summary.vehicles.length > 0 && (
        <div className="table-scroll" style={{ boxShadow: 'none' }}>
          <table className="grid">
            <thead>
              <tr>
                <th>Plate</th>
                <th>Type</th>
                <th>History</th>
              </tr>
            </thead>
            <tbody>
              {summary.vehicles.map((v) => (
                <tr key={v.plate} className={plate === v.plate ? 'selected' : undefined}>
                  <td className="mono">{v.plate}</td>
                  <td>{v.carType}</td>
                  <td>
                    <button className="btn-ghost btn btn-sm" onClick={() => setPlate(v.plate)}>
                      View history
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {plate && <VehicleHistoryPanel plate={plate} onClose={() => setPlate(null)} />}
    </div>
  )
}

function VehicleHistoryPanel({ plate, onClose }: { plate: string; onClose: () => void }) {
  const historyQuery = useQuery({
    queryKey: ['vehicle', plate],
    queryFn: () => api<VehicleHistory>(`/api/v1/vehicles/${plate}`),
    retry: false,
  })

  return (
    <section className="card" aria-label={`History for ${plate}`} style={{ marginTop: '1rem' }}>
      <h3 className="panel-title">
        History · <span className="mono">{plate}</span>{' '}
        <button className="btn-ghost btn btn-sm" onClick={onClose}>
          Close
        </button>
      </h3>
      {historyQuery.isPending && <Loading what="vehicle history" />}
      {historyQuery.isError && (
        <ErrorBox message={(historyQuery.error as Error).message} onRetry={() => historyQuery.refetch()} />
      )}
      {historyQuery.data && historyQuery.data.fines.length === 0 && <Empty what="fines for this vehicle" />}
      {historyQuery.data && historyQuery.data.fines.length > 0 && (
        <div className="table-scroll" style={{ boxShadow: 'none' }}>
          <table className="grid">
            <thead>
              <tr>
                <th className="num">Fine ID</th>
                <th className="num">Total (EGP)</th>
                <th className="num">Violations</th>
              </tr>
            </thead>
            <tbody>
              {historyQuery.data.fines.map((f) => (
                <tr key={f.id}>
                  <td className="num">{f.id}</td>
                  <td className="num">{f.totalAmount}</td>
                  <td className="num">{f.violations}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
