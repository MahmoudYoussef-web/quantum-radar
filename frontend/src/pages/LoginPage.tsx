import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { RadarMark } from './LandingPage'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await login(username, password)
      navigate('/overview')
    } catch {
      setError('Invalid credentials. Check the username and password and try again.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-split">
      <div className="login-brand">
        <Link className="brand" to="/" style={{ color: '#e8e0cf' }} aria-label="Back to site">
          <RadarMark /> QuRadar
        </Link>
        <h1>The enforcement console.</h1>
        <p>
          Sign in to review violations, tune the rule book, manage radar devices and
          look up drivers — everything the API enforces by role, in one place.
        </p>
        <ul>
          <li><b>409</b> on event replay — duplicates never double-fine</li>
          <li><b>Tiers</b> price speeding by how far over the limit</li>
          <li><b>JWT</b> access + rotating refresh, BCrypt passwords</li>
        </ul>
      </div>
      <main className="login-form">
        <form onSubmit={submit} aria-label="Sign in">
          <h2>Sign in</h2>
          <p className="muted" style={{ marginTop: '-0.5rem' }}>
            Sign in to the enforcement console. Authorized personnel only.
          </p>
          <div className="field">
            <label htmlFor="login-username">Username</label>
            <input
              id="login-username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              required
            />
          </div>
          <div className="field">
            <label htmlFor="login-password">Password</label>
            <input
              id="login-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
              aria-describedby={error ? 'login-error' : undefined}
            />
          </div>
          {error && (
            <p className="error-text" id="login-error" role="alert">
              {error}
            </p>
          )}
          <button className="btn" type="submit" disabled={busy}>
            {busy ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
      </main>
    </div>
  )
}
