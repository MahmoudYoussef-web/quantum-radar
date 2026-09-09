import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { RadarMark } from '../pages/LandingPage'
import { ToastProvider } from './Toast'

const TITLES: Record<string, { title: string; crumb: string }> = {
  '/overview': { title: 'Overview', crumb: 'console / overview' },
  '/violations': { title: 'Violations', crumb: 'console / violations' },
  '/rules': { title: 'Rules', crumb: 'console / rules' },
  '/devices': { title: 'Devices', crumb: 'console / devices' },
  '/drivers': { title: 'Drivers & vehicles', crumb: 'console / drivers' },
  '/audit': { title: 'Audit log', crumb: 'console / audit' },
}

export function Layout({ children }: { children: React.ReactNode }) {
  const { username, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const meta = TITLES[location.pathname] ?? { title: 'Console', crumb: 'console' }

  const signOut = () => {
    logout()
    navigate('/')
  }

  return (
    <ToastProvider>
    <div className="app">
      <a className="skip" href="#main">
        Skip to content
      </a>
      <aside className="side">
        <span className="brand" aria-label="QuRadar console">
          <RadarMark size={24} /> QuRadar
        </span>
        <nav aria-label="Console">
          <p className="nav-label">Monitor</p>
          <NavLink to="/overview">Overview</NavLink>
          <NavLink to="/violations">Violations</NavLink>
          <p className="nav-label">Manage</p>
          <NavLink to="/rules">Rules</NavLink>
          <NavLink to="/devices">Devices</NavLink>
          <NavLink to="/drivers">Drivers</NavLink>
          <NavLink to="/audit">Audit log</NavLink>
        </nav>
        <div className="side-user">
          <span className="avatar" aria-hidden="true">
            {(username ?? '?').slice(0, 1)}
          </span>
          <span>{username}</span>
          <button className="btn-ghost btn btn-sm" onClick={signOut}>
            Logout
          </button>
        </div>
      </aside>
      <div className="app-main">
        <header className="topbar">
          <div>
            <div className="crumb" aria-hidden="true">
              {meta.crumb}
            </div>
            <h1>{meta.title}</h1>
          </div>
          <span className="live">api connected</span>
        </header>
        <main id="main">{children}</main>
      </div>
    </div>
    </ToastProvider>
  )
}
