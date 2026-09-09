import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Device } from '../api/types'
import { Empty, ErrorBox, Loading } from '../components/Status'
import { useToast } from '../components/Toast'

export function DevicesPage() {
  const client = useQueryClient()
  const notify = useToast()
  const [code, setCode] = useState('')
  const [name, setName] = useState('')

  const query = useQuery({ queryKey: ['devices'], queryFn: () => api<Device[]>('/api/v1/devices') })
  const refresh = () => client.invalidateQueries({ queryKey: ['devices'] })

  const create = useMutation({
    mutationFn: () =>
      api<Device>('/api/v1/devices', {
        method: 'POST',
        body: JSON.stringify({ deviceCode: code.trim(), name: name.trim() }),
      }),
    onSuccess: (device) => {
      setCode('')
      setName('')
      refresh()
      notify(`Device ${device.deviceCode} registered.`)
    },
  })

  const toggle = useMutation({
    mutationFn: (d: Device) =>
      api<Device>(`/api/v1/devices/${d.deviceCode}`, {
        method: 'PATCH',
        body: JSON.stringify({ active: !d.active }),
      }),
    onSuccess: (device) => {
      refresh()
      notify(`Device ${device.deviceCode} ${device.active ? 'activated' : 'deactivated'}.`)
    },
  })

  return (
    <div>
      <div className="page-head">
        <h1>Devices</h1>
        <p>Registered radars. A deactivated device gets 403 on ingest — its events stop cold.</p>
      </div>
      <form
        className="filters"
        aria-label="Register device"
        onSubmit={(e) => {
          e.preventDefault()
          create.mutate()
        }}
      >
        <div className="field">
          <label htmlFor="d-code">Device code</label>
          <input id="d-code" placeholder="RADAR-002" value={code} onChange={(e) => setCode(e.target.value)} autoComplete="off" />
        </div>
        <div className="field">
          <label htmlFor="d-name">Name</label>
          <input id="d-name" placeholder="North gate radar" value={name} onChange={(e) => setName(e.target.value)} autoComplete="off" />
        </div>
        <button className="btn" type="submit">
          Register
        </button>
      </form>
      {query.isPending && <Loading what="devices" />}
      {query.isError && <ErrorBox message={(query.error as Error).message} onRetry={() => query.refetch()} />}
      {query.data && query.data.length === 0 && <Empty what="devices" />}
      {query.data && query.data.length > 0 && (
        <div className="table-scroll">
          <table className="grid">
            <thead>
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {query.data.map((d) => (
                <tr key={d.deviceCode}>
                  <td className="mono">{d.deviceCode}</td>
                  <td>{d.name}</td>
                  <td>
                    <span className={d.active ? 'pill pill-on' : 'pill pill-bad'}>
                      {d.active ? 'ACTIVE' : 'DEACTIVATED'}
                    </span>
                  </td>
                  <td>
                    <button className="btn-ghost btn btn-sm" onClick={() => toggle.mutate(d)}>
                      {d.active ? 'Deactivate' : 'Activate'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {(create.isError || toggle.isError) && (
        <p className="error-text">{((create.error ?? toggle.error) as Error).message}</p>
      )}
    </div>
  )
}
