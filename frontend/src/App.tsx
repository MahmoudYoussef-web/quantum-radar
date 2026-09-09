import { HashRouter, Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { Layout } from './components/Layout'
import { LandingPage } from './pages/LandingPage'
import { AuditPage } from './pages/AuditPage'
import { LoginPage } from './pages/LoginPage'
import { OverviewPage } from './pages/OverviewPage'
import { ViolationsPage } from './pages/ViolationsPage'
import { RulesPage } from './pages/RulesPage'
import { DevicesPage } from './pages/DevicesPage'
import { DriversPage } from './pages/DriversPage'
import { tokens } from './api/client'

function Guard() {
  if (!tokens.access) return <Navigate to="/" replace />
  return <Outlet />
}

function Shell() {
  return (
    <Layout>
      <Outlet />
    </Layout>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <HashRouter>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route element={<Guard />}>
            <Route element={<Shell />}>
              <Route path="/overview" element={<OverviewPage />} />
              <Route path="/violations" element={<ViolationsPage />} />
              <Route path="/rules" element={<RulesPage />} />
              <Route path="/devices" element={<DevicesPage />} />
              <Route path="/drivers" element={<DriversPage />} />
              <Route path="/audit" element={<AuditPage />} />
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </HashRouter>
    </AuthProvider>
  )
}
