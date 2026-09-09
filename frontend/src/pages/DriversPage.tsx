import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { DriverSummary, VehicleHistory } from '../api/types'
import { Empty, ErrorBox, Loading } from '../components/Status'

export function DriversPage() {
  const [license, setLicense] = useState('')
  const [wanted, setWanted] = useState<string | null>(null)

  const summaryQuery = useQuery({
    queryKey: ['driver-summary', wanted],
    queryFn: () => api<DriverSummary>(`/api/v1/drivers/${wanted}/summary`),
    enabled: wanted !== null,
    retry: false,
  })

  return (
    <div>
      <div className="page-head">
        <h1>Drivers &amp; vehicles</h1>
        <p>Look up a driver for identity, vehicles and enforcement totals — then drill into any plate.</p>
      </div>
      <form
        className="filters"
        aria-label="Look up driver"
        onSubmit={(e) => {
          e.preventDefault()
          setWanted(license.trim() || null)
        }}
      >
        <div className="field">
          <label htmlFor="dr-license">License no</label>
          <input
            id="dr-license"
            placeholder="AHM-0001"
            value={license}
            onChange={(e) => setLicense(e.target.value)}
            autoComplete="off"
          />
        </div>
        <button className="btn" type="submit">
          Lookup
        </button>
      </form>
      {wanted === null && <p className="muted">Enter a license number to look up a driver.</p>}
      {summaryQuery.isPending && wanted !== null && <Loading what="driver" />}
      {summaryQuery.isError && <ErrorBox message={(summaryQuery.error as Error).message} onRetry={() => summaryQuery.refetch()} />}
      {summaryQuery.data && <DriverCard summary={summaryQuery.data} />}
    </div>
  )
}

function DriverCard({ summary }: { summary: DriverSummary }) {
  const { driver } = summary
  const [plate, setPlate] = useState<string | null>(null)

  return (
    <div>
      <section className="card" aria-label={`Driver ${driver.name}`}>
        <h2 className="panel-title">Driver</h2>
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
      </section>

      <section className="card" aria-label="Vehicles" style={{ marginTop: '1rem' }}>
        <h2 className="panel-title">Vehicles ({summary.vehicles.length})</h2>
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
      </section>

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
      <h2 className="panel-title">
        History · <span className="mono">{plate}</span>{' '}
        <button className="btn-ghost btn btn-sm" onClick={onClose}>
          Close
        </button>
      </h2>
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
