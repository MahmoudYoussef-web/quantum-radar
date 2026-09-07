import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Driver, VehicleHistory } from '../api/types'
import { Empty, ErrorBox, Loading } from '../components/Status'

export function DriversPage() {
  const [license, setLicense] = useState('')
  const [wanted, setWanted] = useState<string | null>(null)

  const driverQuery = useQuery({
    queryKey: ['driver', wanted],
    queryFn: () => api<Driver>(`/api/v1/drivers/${wanted}`),
    enabled: wanted !== null,
    retry: false,
  })

  return (
    <div>
      <div className="page-head">
        <h1>Drivers &amp; vehicles</h1>
        <p>Look up a driver by license number, then pull any plate&apos;s fine history.</p>
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
      {driverQuery.isPending && wanted !== null && <Loading what="driver" />}
      {driverQuery.isError && <ErrorBox message={(driverQuery.error as Error).message} onRetry={() => driverQuery.refetch()} />}
      {driverQuery.data && <DriverDetail licenseNo={driverQuery.data.licenseNo} driver={driverQuery.data} />}
    </div>
  )
}

function DriverDetail({ licenseNo, driver }: { licenseNo: string; driver: Driver }) {
  const [plate, setPlate] = useState('')
  const [wantedPlate, setWantedPlate] = useState<string | null>(null)

  const historyQuery = useQuery({
    queryKey: ['vehicle', wantedPlate],
    queryFn: () => api<VehicleHistory>(`/api/v1/vehicles/${wantedPlate}`),
    enabled: wantedPlate !== null,
    retry: false,
  })

  return (
    <section className="card" aria-label={`Driver ${driver.name}`}>
      <h2 className="panel-title">
        {driver.name} <span className="mono muted">{licenseNo}</span>
      </h2>
      <p>
        <span className={driver.licenseStatus === 'ACTIVE' ? 'pill pill-on' : 'pill pill-bad'}>
          {driver.licenseStatus ?? 'NO LICENSE'}
        </span>{' '}
        <span className="mono">{driver.penaltyPoints} pts</span>{' '}
        <span className="muted">record v{driver.version}</span>
      </p>
      <form
        className="filters"
        aria-label="Vehicle history"
        onSubmit={(e) => {
          e.preventDefault()
          setWantedPlate(plate.trim() || null)
        }}
      >
        <div className="field">
          <label htmlFor="vh-plate">Plate</label>
          <input id="vh-plate" placeholder="ABC1234" value={plate} onChange={(e) => setPlate(e.target.value)} autoComplete="off" />
        </div>
        <button className="btn" type="submit">
          Vehicle history
        </button>
      </form>
      {historyQuery.isPending && wantedPlate !== null && <Loading what="vehicle history" />}
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
