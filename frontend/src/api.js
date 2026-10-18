// Thin fetch wrapper. Errors carry the backend's message and per-field details.
const BASE = import.meta.env.VITE_API_URL || ''

async function request(path, options = {}) {
  let res
  try {
    res = await fetch(`${BASE}/api${path}`, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
      body: options.body ? JSON.stringify(options.body) : undefined,
    })
  } catch {
    throw new Error('Cannot reach the backend. Is it running on http://localhost:8080?')
  }
  if (res.status === 204) return null
  const data = await res.json().catch(() => null)
  if (!res.ok) {
    const err = new Error(data?.message || `Request failed (${res.status})`)
    err.details = data?.details || {}
    throw err
  }
  return data
}

export const api = {
  health: () => request('/health'),
  coins: () => request('/coins'),
  portfolio: () => request('/portfolio'),
  summary: () => request('/portfolio/summary'),
  analytics: () => request('/analytics'),
  addHolding: (b) => request('/holdings', { method: 'POST', body: b }),
  deleteHolding: (coinId) => request(`/holdings/${coinId}`, { method: 'DELETE' }),
  transactions: () => request('/transactions'),
  addTransaction: (b) => request('/transactions', { method: 'POST', body: b }),
  updateTransaction: (id, b) => request(`/transactions/${id}`, { method: 'PUT', body: b }),
  deleteTransaction: (id) => request(`/transactions/${id}`, { method: 'DELETE' }),
  alerts: () => request('/alerts'),
  addAlert: (b) => request('/alerts', { method: 'POST', body: b }),
  deleteAlert: (id) => request(`/alerts/${id}`, { method: 'DELETE' }),
  insights: (ai = false) => request(`/insights?ai=${ai}`),
  seed: () => request('/dev/seed', { method: 'POST' }),
  reset: () => request('/dev/reset', { method: 'DELETE' }),
}
