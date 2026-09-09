import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import type { Device, Page, RuleConfig, ViolationSummary } from '../api/types'
import { Empty, ErrorBox, Loading } from '../components/Status'
import { RuleBars, TrendChart } from '../components/Charts'

export function OverviewPage() {
  const fines = useQuery({
    queryKey: ['overview-fines'],
    queryFn: () => api<Page<{ id: number }>>('/api/v1/fines?page=0&size=1'),
  })
  const violations = useQuery({
    queryKey: ['overview-violations'],
    queryFn: () => api<Page<ViolationSummary>>('/api/v1/violations?page=0&size=5'),
  })
  const rules = useQuery({
    queryKey: ['rules'],
    queryFn: () => api<RuleConfig[]>('/api/v1/rules'),
  })
  const devices = useQuery({
    queryKey: ['devices'],
    queryFn: () => api<Device[]>('/api/v1/devices'),
  })
  const daily = useQuery({
    queryKey: ['stats-daily'],
    queryFn: () => api<{ date: string; count: number }[]>('/api/v1/violations/stats/daily?days=14'),
  })
  const byRule = useQuery({
    queryKey: ['stats-by-rule'],
    queryFn: () => api<{ rule: string; count: number }[]>('/api/v1/violations/stats/by-rule'),
  })

  const all = [fines, violations, rules, devices, daily, byRule]
  const pending = all.some((q) => q.isPending)
  const failed = all.find((q) => q.isError)
  const refetchAll = () => all.forEach((q) => q.refetch())

  const enabledRules = rules.data?.filter((r) => r.enabled).length ?? 0
  const activeDevices = devices.data?.filter((d) => d.active).length ?? 0

  return (
    <div>
      <div className="page-head">
        <h1>Shift overview</h1>
        <p>What the network recorded, what the engine enforces, and what is watching.</p>
      </div>

      {pending && <Loading what="overview" />}
      {failed && <ErrorBox message={(failed.error as Error).message} onRetry={refetchAll} />}

      {!pending && !failed && (
        <>
          <ul className="kpis">
            <li className="kpi">
              <span>Distinct fines</span>
              <b>{fines.data?.totalElements ?? 0}</b>
              <small>one per fined observation</small>
            </li>
            <li className="kpi">
              <span>Violations</span>
              <b>{violations.data?.totalElements ?? 0}</b>
              <small>individual rule hits — several can form one fine</small>
            </li>
            <li className="kpi">
              <span>Rules enabled</span>
              <b>
                {enabledRules}
                <span className="muted" style={{ fontSize: '1rem' }}>/{rules.data?.length ?? 0}</span>
              </b>
              <small>DB-configured, live now</small>
            </li>
            <li className="kpi">
              <span>Devices active</span>
              <b>
                {activeDevices}
                <span className="muted" style={{ fontSize: '1rem' }}>/{devices.data?.length ?? 0}</span>
              </b>
              <small>reporting radars</small>
            </li>
          </ul>

          <div className="split-2">
            <section className="card" aria-label="Violations over time">
              <h2 className="panel-title">Violations over time</h2>
              <TrendChart points={daily.data ?? []} days={14} />
            </section>
            <section className="card" aria-label="Violations by rule">
              <h2 className="panel-title">Violations by rule</h2>
              <RuleBars rows={byRule.data ?? []} />
              <p style={{ marginBottom: 0 }}>
                <Link to="/rules">Manage rules →</Link>
              </p>
            </section>
          </div>

          <section className="card" aria-label="Latest violations" style={{ marginTop: '1rem' }}>
            <h2 className="panel-title">Latest violations</h2>
            {(violations.data?.content.length ?? 0) === 0 && <Empty what="violations" />}
            {(violations.data?.content.length ?? 0) > 0 && (
              <div className="table-scroll" style={{ boxShadow: 'none' }}>
                <table className="grid">
                  <thead>
                    <tr>
                      <th>Plate</th>
                      <th>Rule</th>
                      <th className="num">Fee</th>
                    </tr>
                  </thead>
                  <tbody>
                    {violations.data?.content.map((v) => (
                      <tr key={v.id}>
                        <td className="mono">{v.plateNumber}</td>
                        <td className="mono">{v.ruleName}</td>
                        <td className="num">{v.fee}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <p style={{ marginBottom: 0 }}>
              <Link to="/violations">Open the full table →</Link>
            </p>
          </section>
        </>
      )}
    </div>
  )
}
