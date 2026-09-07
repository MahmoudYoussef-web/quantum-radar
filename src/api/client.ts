import { API_URL } from './types'

const ACCESS_KEY = 'qr.access'
const REFRESH_KEY = 'qr.refresh'

export const tokens = {
  get access() {
    return localStorage.getItem(ACCESS_KEY)
  },
  get refresh() {
    return localStorage.getItem(REFRESH_KEY)
  },
  set(access: string, refresh: string) {
    localStorage.setItem(ACCESS_KEY, access)
    localStorage.setItem(REFRESH_KEY, refresh)
  },
  clear() {
    localStorage.removeItem(ACCESS_KEY)
    localStorage.removeItem(REFRESH_KEY)
  },
}

async function refreshAccess(): Promise<string | null> {
  const refreshToken = tokens.refresh
  if (!refreshToken) return null
  const res = await fetch(`${API_URL}/api/v1/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
  if (!res.ok) {
    tokens.clear()
    return null
  }
  const pair = await res.json()
  tokens.set(pair.accessToken, pair.refreshToken)
  return pair.accessToken
}

/** fetch with Bearer auth + one silent refresh retry on 401. */
export async function api<T>(path: string, init?: RequestInit, retried = false): Promise<T> {
  const headers: Record<string, string> = { ...(init?.headers as Record<string, string>) }
  if (tokens.access) headers['Authorization'] = `Bearer ${tokens.access}`
  if (init?.body !== undefined) headers['Content-Type'] = 'application/json'

  const res = await fetch(`${API_URL}${path}`, { ...init, headers })
  if (res.status === 401 && !retried) {
    const access = await refreshAccess()
    if (access) return api<T>(path, init, true)
    window.location.hash = '#/login'
    throw new Error('Unauthorized')
  }
  if (res.status === 204) return undefined as T
  const body = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error((body as { error?: string }).error ?? `HTTP ${res.status}`)
  return body as T
}
