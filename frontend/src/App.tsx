import { HashRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { Layout } from './components/Layout'
import { LoginPage } from './pages/LoginPage'
import { ViolationsPage } from './pages/ViolationsPage'
import { RulesPage } from './pages/RulesPage'
import { DevicesPage } from './pages/DevicesPage'
import { DriversPage } from './pages/DriversPage'
import { tokens } from './api/client'

function Guard({ children }: { children: JSX.Element }) {
  if (!tokens.access) return <Navigate to="/login" replace />
  return children
}

function Shell() {
  return (
    <Layout>
      <Routes>
        <Route path="/violations" element={<ViolationsPage />} />
        <Route path="/rules" element={<RulesPage />} />
        <Route path="/devices" element={<DevicesPage />} />
        <Route path="/drivers" element={<DriversPage />} />
        <Route path="*" element={<Navigate to="/violations" replace />} />
      </Routes>
    </Layout>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <HashRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/*"
            element={
              <Guard>
                <Shell />
              </Guard>
            }
          />
        </Routes>
      </HashRouter>
    </AuthProvider>
  )
}
