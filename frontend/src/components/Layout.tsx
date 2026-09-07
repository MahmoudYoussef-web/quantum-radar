import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function Layout({ children }: { children: React.ReactNode }) {
  const { username, logout } = useAuth()
  const navigate = useNavigate()

  const signOut = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="shell">
      <nav>
        <strong>QuRadar</strong>
        <Link to="/violations">Violations</Link>
        <Link to="/rules">Rules</Link>
        <Link to="/devices">Devices</Link>
        <Link to="/drivers">Drivers</Link>
        <span className="spacer" />
        <span className="muted">{username}</span>
        <button onClick={signOut}>Logout</button>
      </nav>
      <main>{children}</main>
    </div>
  )
}
