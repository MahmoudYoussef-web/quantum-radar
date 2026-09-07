import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Device } from '../api/types'
import { Empty, ErrorBox, Loading } from '../components/Status'

export function DevicesPage() {
  const client = useQueryClient()
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
    onSuccess: () => {
      setCode('')
      setName('')
      refresh()
    },
  })

  const toggle = useMutation({
    mutationFn: (d: Device) =>
      api<Device>(`/api/v1/devices/${d.deviceCode}`, {
        method: 'PATCH',
        body: JSON.stringify({ active: !d.active }),
      }),
    onSuccess: refresh,
  })

  return (
    <div>
      <h2>Devices</h2>
      <form
        className="filters"
        onSubmit={(e) => {
          e.preventDefault()
          create.mutate()
        }}
      >
        <input placeholder="Device code" value={code} onChange={(e) => setCode(e.target.value)} />
        <input placeholder="Name" value={name} onChange={(e) => setName(e.target.value)} />
        <button type="submit">Register</button>
      </form>
      {query.isPending && <Loading what="devices" />}
      {query.isError && <ErrorBox message={(query.error as Error).message} onRetry={() => query.refetch()} />}
      {query.data && query.data.length === 0 && <Empty what="devices" />}
      {query.data && query.data.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Code</th>
              <th>Name</th>
              <th>Active</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {query.data.map((d) => (
              <tr key={d.deviceCode}>
                <td>{d.deviceCode}</td>
                <td>{d.name}</td>
                <td>{d.active ? 'yes' : 'no'}</td>
                <td>
                  <button onClick={() => toggle.mutate(d)}>
                    {d.active ? 'Deactivate' : 'Activate'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
