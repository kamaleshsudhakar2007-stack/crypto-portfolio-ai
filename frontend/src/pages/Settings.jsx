import { useState } from 'react'
import { api } from '../api'
import { useApi } from '../lib/useApi'
import { Card, ConfirmDialog, PageHeader } from '../components/ui'
import { useNotify } from '../components/Notify'

export default function SettingsPage() {
  const notify = useNotify()
  const health = useApi(api.health)
  const [confirmReset, setConfirmReset] = useState(false)
  const [busy, setBusy] = useState(false)

  const run = async (fn, okMsg) => {
    setBusy(true)
    try { await fn(); notify(okMsg, 'success') } catch (e) { notify(e.message, 'error') } finally { setBusy(false); setConfirmReset(false); health.reload() }
  }
  const h = health.data

  return (
    <>
      <PageHeader title="Settings / About" />
      <div className="grid gap-4 lg:grid-cols-2">
        <Card title="System status">
          <dl className="space-y-2 text-sm">
            <Row k="Backend API" v={health.error ? 'Unreachable' : h ? 'Reachable' : '…'} bad={!!health.error} />
            <Row k="MongoDB" v={h ? h.database : '—'} bad={h && h.database !== 'UP'} />
            <Row k="AI key configured" v={h ? (h.aiConfigured ? 'Yes (optional AI summaries available)' : 'No (rule-based insights only)') : '—'} />
          </dl>
        </Card>
        <Card title="Demo data">
          <p className="mb-3 text-sm text-slate-400">Load five sample transactions (BTC, ETH, SOL buys and an ETH sale) to explore the app. These are sample records you can delete, not real trades.</p>
          <div className="flex flex-wrap gap-2">
            <button className="btn-primary" disabled={busy} onClick={() => run(api.seed, 'Sample data loaded')}>Load sample data</button>
            <button className="btn-danger" disabled={busy} onClick={() => setConfirmReset(true)}>Delete all data</button>
          </div>
        </Card>
        <Card title="About" className="lg:col-span-2">
          <div className="space-y-2 text-sm text-slate-300">
            <p><strong className="text-ink">Crypto Portfolio AI</strong> is a college project: React + Vite + Tailwind frontend, Spring Boot REST API, MongoDB storage, CoinGecko market data.</p>
            <p><strong className="text-ink">Local demo app.</strong> There is no authentication, so all data is shared by anyone who can open this instance. It is not isolated between users.</p>
            <p><strong className="text-ink">Cost basis:</strong> weighted-average cost. Buy fees are added to cost; sell fees reduce proceeds. Unrealized and realized profit/loss are always shown separately.</p>
            <p><strong className="text-ink">Market data:</strong> labelled LIVE, STALE or DEMO. Prices can be delayed up to a minute. Insights are educational and are not financial advice. No real trading, wallets or private keys are involved.</p>
          </div>
        </Card>
      </div>
      {confirmReset && (
        <ConfirmDialog title="Delete all data?" confirmLabel="Delete everything" busy={busy} onCancel={() => setConfirmReset(false)}
          onConfirm={() => run(api.reset, 'All data deleted')} text="This permanently deletes every transaction, alert and snapshot. This cannot be undone." />
      )}
    </>
  )
}

const Row = ({ k, v, bad }) => (
  <div className="flex justify-between gap-4 border-b border-line pb-2"><dt className="text-slate-400">{k}</dt><dd className={bad ? 'text-red-400' : 'text-ink'}>{v}</dd></div>
)
