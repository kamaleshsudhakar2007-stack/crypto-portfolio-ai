import { Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import Portfolio from './pages/Portfolio'
import Markets from './pages/Markets'
import Transactions from './pages/Transactions'
import Analytics from './pages/Analytics'
import Alerts from './pages/Alerts'
import Insights from './pages/Insights'
import SettingsPage from './pages/Settings'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Dashboard />} />
        <Route path="portfolio" element={<Portfolio />} />
        <Route path="markets" element={<Markets />} />
        <Route path="transactions" element={<Transactions />} />
        <Route path="analytics" element={<Analytics />} />
        <Route path="alerts" element={<Alerts />} />
        <Route path="insights" element={<Insights />} />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="*" element={<p className="p-6 text-slate-400">Page not found.</p>} />
      </Route>
    </Routes>
  )
}
