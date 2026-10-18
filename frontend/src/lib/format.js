// Single source of truth for money display. Everything is INR (Indian grouping: ₹1,25,000.00).
// The API already returns INR (CoinGecko is queried with vs_currency=inr) — never convert here.
const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 2, maximumFractionDigits: 2 })
const inrSmall = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 2, maximumFractionDigits: 8 })
const compact = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', notation: 'compact', maximumFractionDigits: 2 })
const num = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 8 })
const ok = (v) => v != null && Number.isFinite(Number(v))

export const money = (v) => (ok(v) ? inr.format(v) : '—') // negatives render as -₹2,500.00
// Coin prices: keep precision for small-priced tokens
export const price = (v) => (!ok(v) ? '—' : Math.abs(v) < 100 ? inrSmall.format(v) : inr.format(v))
export const bigMoney = (v) => (ok(v) ? compact.format(v) : '—')
export const qty = (v) => (ok(v) ? num.format(v) : '—')
export const pct = (v) => (ok(v) ? `${v > 0 ? '+' : ''}${Number(v).toFixed(2)}%` : '—')
export const signedMoney = (v) => (ok(v) ? `${v > 0 ? '+' : ''}${inr.format(v)}` : '—')
export const tone = (v) => (!ok(v) || v === 0 ? 'text-slate-400' : v > 0 ? 'text-emerald-400' : 'text-red-400')
export const dateTime = (iso) => (iso ? new Date(iso).toLocaleString('en-IN') : '—')
export const toLocalInput = (d = new Date()) => {
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`
}
