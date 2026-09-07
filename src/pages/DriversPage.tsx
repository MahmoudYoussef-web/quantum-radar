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
      <h2>Drivers & vehicles</h2>
      <form
        className="filters"
        onSubmit={(e) => {
          e.preventDefault()
          setWanted(license.trim() || null)
        }}
      >
        <input
          placeholder="License no (e.g. AHM-0001)"
          value={license}
          onChange={(e) => setLicense(e.target.value)}
        />
        <button type="submit">Lookup</button>
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
    <div className="card">
      <h3>
        {driver.name} ({licenseNo})
      </h3>
      <p>
        License: {driver.licenseStatus ?? '—'} · Points: {driver.penaltyPoints} (v
        {driver.version})
      </p>
      <form
        className="filters"
        onSubmit={(e) => {
          e.preventDefault()
          setWantedPlate(plate.trim() || null)
        }}
      >
        <input placeholder="Plate (e.g. ABC1234)" value={plate} onChange={(e) => setPlate(e.target.value)} />
        <button type="submit">Vehicle history</button>
      </form>
      {historyQuery.isPending && wantedPlate !== null && <Loading what="vehicle history" />}
      {historyQuery.isError && (
        <ErrorBox message={(historyQuery.error as Error).message} onRetry={() => historyQuery.refetch()} />
      )}
      {historyQuery.data && historyQuery.data.fines.length === 0 && <Empty what="fines for this vehicle" />}
      {historyQuery.data && historyQuery.data.fines.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Fine ID</th>
              <th>Total</th>
              <th>Violations</th>
            </tr>
          </thead>
          <tbody>
            {historyQuery.data.fines.map((f) => (
              <tr key={f.id}>
                <td>{f.id}</td>
                <td>{f.totalAmount}</td>
                <td>{f.violations}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
