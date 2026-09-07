import { createContext, useCallback, useContext, useState } from 'react'
import { API_URL } from '../api/types'
import { tokens } from '../api/client'

interface AuthState {
  username: string | null
  login(username: string, password: string): Promise<void>
  logout(): void
}

const AuthContext = createContext<AuthState>({
  username: null,
  login: async () => {},
  logout: () => {},
})

function decodeUsername(access: string): string | null {
  try {
    return JSON.parse(atob(access.split('.')[1])).sub ?? null
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [username, setUsername] = useState<string | null>(() =>
    tokens.access ? decodeUsername(tokens.access) : null,
  )

  const login = useCallback(async (name: string, password: string) => {
    const res = await fetch(`${API_URL}/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: name, password }),
    })
    if (!res.ok) throw new Error('Invalid credentials')
    const pair = await res.json()
    tokens.set(pair.accessToken, pair.refreshToken)
    setUsername(decodeUsername(pair.accessToken))
  }, [])

  const logout = useCallback(() => {
    const refreshToken = tokens.refresh
    tokens.clear()
    setUsername(null)
    if (refreshToken) {
      fetch(`${API_URL}/api/v1/auth/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      }).catch(() => {})
    }
  }, [])

  return <AuthContext.Provider value={{ username, login, logout }}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
