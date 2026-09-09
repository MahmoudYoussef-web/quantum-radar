import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Device, DeviceDetail } from '../api/types'
import { Empty, ErrorBox, Loading } from '../components/Status'
import { Confirm } from '../components/Confirm'
import { useToast } from '../components/Toast'

export function ago(iso: string | null): string {
  if (!iso) return 'never'
  const seconds = Math.max(0, Math.floor((Date.now() - new Date(iso).getTime()) / 1000))
  if (seconds < 60) return `${seconds} sec ago`
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes} min ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 48) return `${hours} h ago`
  return `${Math.floor(hours / 24)} d ago`
}

export function fmtDateTime(iso: string): string {
  return new Date(iso).toLocaleString('en-GB', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function healthPill(health: Device['health']): string {
  if (health === 'ACTIVE') return 'pill pill-on'
  if (health === 'DEGRADED') return 'pill pill-warn'
  return 'pill pill-bad'
}

export function DevicesPage() {
  const client = useQueryClient()
  const notify = useToast()
  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [detailCode, setDetailCode] = useState<string | null>(null)
  const [deactivating, setDeactivating] = useState<Device | null>(null)

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
        <p>
          Registered radars. Lifecycle says whether a device may report;
          connectivity says whether it actually did recently. A deactivated device
          gets 403 on ingest — an offline one simply stopped reporting.
        </p>
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
                  <th>Lifecycle</th>
                  <th>Connectivity</th>
                  <th>Last seen</th>
                  <th>Actions</th>
                </tr>
            </thead>
            <tbody>
              {query.data.map((d) => (
                <tr key={d.deviceCode} className={detailCode === d.deviceCode ? 'selected' : undefined}>
                  <td className="mono">{d.deviceCode}</td>
                  <td>{d.name}</td>
                  <td>
                    <span className={d.active ? 'pill pill-on' : 'pill pill-bad'}>
                      {d.active ? 'ACTIVE' : 'DEACTIVATED'}
                    </span>
                  </td>
                  <td>
                    <span className={healthPill(d.health)}>{d.health}</span>
                  </td>
                  <td className="mono">{ago(d.lastSeenAt)}</td>
                  <td>
                    <span style={{ display: 'flex', gap: '0.4rem' }}>
                      <button
                        className="btn-ghost btn btn-sm"
                        onClick={() => setDetailCode(detailCode === d.deviceCode ? null : d.deviceCode)}
                      >
                        Details
                      </button>
                    <button
                      className="btn-ghost btn btn-sm"
                      onClick={() => {
                        if (d.active) {
                          setDeactivating(d)
                        } else {
                          toggle.mutate(d)
                        }
                      }}
                    >
                      {d.active ? 'Deactivate' : 'Activate'}
                    </button>
                    </span>
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
      {detailCode && <DeviceDetails code={detailCode} onClose={() => setDetailCode(null)} />}
      {deactivating && (
        <Confirm
          title={`Deactivate ${deactivating.deviceCode}?`}
          body="Its events will be rejected with 403 until reactivated. Recorded history is unaffected."
          confirmLabel="Deactivate device"
          onClose={() => setDeactivating(null)}
          onConfirm={() => toggle.mutate(deactivating)}
        />
      )}
    </div>
  )
}

function DeviceDetails({ code, onClose }: { code: string; onClose: () => void }) {
  const detailQuery = useQuery({
    queryKey: ['device', code],
    queryFn: () => api<DeviceDetail>(`/api/v1/devices/${code}`),
    retry: false,
  })

  return (
    <section className="card" aria-label={`Device ${code}`} style={{ marginTop: '1rem' }}>
      <h2 className="panel-title">
        Device · <span className="mono">{code}</span>{' '}
        <button className="btn-ghost btn btn-sm" onClick={onClose}>
          Close
        </button>
      </h2>
      {detailQuery.isPending && <Loading what="device details" />}
      {detailQuery.isError && (
        <ErrorBox message={(detailQuery.error as Error).message} onRetry={() => detailQuery.refetch()} />
      )}
      {detailQuery.data && (
        <dl className="facts">
          <div>
            <dt>Lifecycle</dt>
            <dd>
              <span className={detailQuery.data.active ? 'pill pill-on' : 'pill pill-bad'}>
                {detailQuery.data.active ? 'ACTIVE' : 'DEACTIVATED'}
              </span>
            </dd>
          </div>
          <div>
            <dt>Connectivity</dt>
            <dd>
              <span className={healthPill(detailQuery.data.health)}>{detailQuery.data.health}</span>
            </dd>
          </div>
          <div>
            <dt>Last heartbeat</dt>
            <dd className="mono">{detailQuery.data.lastSeenAt ? fmtDateTime(detailQuery.data.lastSeenAt) : 'never'}</dd>
          </div>
          <div>
            <dt>Firmware</dt>
            <dd className="mono">{detailQuery.data.firmwareVersion ?? '—'}</dd>
          </div>
          <div>
            <dt>Last IP</dt>
            <dd className="mono">{detailQuery.data.lastIp ?? '—'}</dd>
          </div>
          <div>
            <dt>Events ingested</dt>
            <dd className="mono">{detailQuery.data.eventCount.toLocaleString('en-EG')}</dd>
          </div>
          <div>
            <dt>Last event</dt>
            <dd className="mono">{detailQuery.data.lastEventAt ? fmtDateTime(detailQuery.data.lastEventAt) : '—'}</dd>
          </div>
          <div>
            <dt>Registered</dt>
            <dd className="mono">{fmtDateTime(detailQuery.data.registeredAt)}</dd>
          </div>
        </dl>
      )}
    </section>
  )
}
