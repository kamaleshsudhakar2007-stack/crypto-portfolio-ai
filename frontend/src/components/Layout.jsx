import { useEffect, useRef } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { Bell, Bot, ChartPie, Coins, LayoutDashboard, Receipt, Settings, Wallet } from 'lucide-react'
import { api } from '../api'
import { useApi } from '../lib/useApi'
import { useNotify } from './Notify'
import { money } from '../lib/format'

const NAV = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/portfolio', label: 'My Portfolio', icon: Wallet },
  { to: '/markets', label: 'Markets', icon: Coins },
  { to: '/transactions', label: 'Transactions', icon: Receipt },
  { to: '/analytics', label: 'Analytics', icon: ChartPie },
  { to: '/alerts', label: 'Alerts', icon: Bell },
  { to: '/insights', label: 'AI Insights', icon: Bot },
  { to: '/settings', label: 'Settings / About', icon: Settings },
]

/** Polls alerts every 60s while the app is open and shows an in-app toast for newly triggered ones. */
function useAlertWatcher() {
  const notify = useNotify()
  const seen = useRef(null)
  const { data } = useApi(api.alerts, { pollMs: 60000 })
  useEffect(() => {
    if (!data) return
    const triggered = data.alerts.filter((a) => a.triggeredAt)
    if (seen.current === null) {
      seen.current = new Set(triggered.map((a) => a.id)) // don't re-announce old ones on page load
      return
    }
    triggered.filter((a) => !seen.current.has(a.id)).forEach((a) => {
      seen.current.add(a.id)
      notify(`${a.symbol} went ${a.direction === 'ABOVE' ? 'above' : 'below'} ${money(a.targetPrice)} (now ${money(a.triggeredPrice)})`, 'info')
    })
  }, [data, notify])
}

export default function Layout() {
  useAlertWatcher()
  const { data: health, error } = useApi(api.health, { pollMs: 30000 })
  const backendDown = !!error
  const dbDown = health && health.database !== 'UP'

  return (
    <div className="min-h-screen md:flex md:gap-5 md:p-5">
      <aside className="hidden w-60 shrink-0 md:block">
        <div className="glass sticky top-5 flex h-[calc(100vh-2.5rem)] flex-col rounded-[28px] p-4">
          <div className="mb-6 flex items-center gap-2 px-2 text-lg font-bold text-ink">
            <span className="grid h-8 w-8 place-items-center rounded-xl bg-brand text-white">₿</span> Crypto Portfolio AI
          </div>
          <nav className="flex flex-col gap-1" aria-label="Main">
            {NAV.map(({ to, label, icon: Icon, end }) => (
              <NavLink key={to} to={to} end={end}
                className={({ isActive }) => `flex items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium transition ${isActive ? 'glass-strong bg-white/80 text-ink shadow-sm' : 'text-slate-300 hover:bg-white/60 hover:text-ink'}`}>
                <Icon size={18} /> {label}
              </NavLink>
            ))}
          </nav>
          <p className="mt-auto px-2 text-xs text-slate-500">All amounts in ₹ INR. Local demo app, no login.</p>
        </div>
      </aside>

      <div className="min-w-0 flex-1 pb-24 md:pb-0">
        {(backendDown || dbDown) && (
          <div role="alert" className="mb-3 rounded-2xl bg-red-400 px-4 py-2 text-center text-sm text-white">
            {backendDown ? 'Backend is not reachable on port 8080. Start the Spring Boot server.' : 'MongoDB is not connected. Start MongoDB or check MONGODB_URI.'}
          </div>
        )}
        <main className="mx-auto max-w-7xl p-4 md:p-2"><Outlet /></main>
      </div>

      <nav className="glass fixed inset-x-3 bottom-3 z-40 flex overflow-x-auto rounded-[26px] p-1.5 md:hidden" aria-label="Mobile">
        {NAV.map(({ to, label, icon: Icon, end }) => (
          <NavLink key={to} to={to} end={end}
            className={({ isActive }) => `flex min-w-[4.5rem] flex-1 flex-col items-center gap-1 rounded-2xl px-2 py-2 text-[10px] ${isActive ? 'bg-white/80 text-ink' : 'text-slate-500'}`}>
            <Icon size={20} /> <span className="whitespace-nowrap">{label.replace('Settings / About', 'Settings').replace('My Portfolio', 'Portfolio')}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
