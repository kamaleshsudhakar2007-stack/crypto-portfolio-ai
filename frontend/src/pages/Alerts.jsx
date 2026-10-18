import { useState } from 'react'
import { BellRing, Plus, Trash2 } from 'lucide-react'
import { api } from '../api'
import { useApi } from '../lib/useApi'
import { Card, ConfirmDialog, Empty, ErrorBox, Field, Modal, Notice, PageHeader, SkeletonTable, SourceBadge } from '../components/ui'
import { useNotify } from '../components/Notify'
import { dateTime, money } from '../lib/format'

export default function Alerts() {
  const notify = useNotify()
  const al = useApi(api.alerts, { pollMs: 60000 })
  const coins = useApi(api.coins)
  const [open, setOpen] = useState(false)
  const [deleting, setDeleting] = useState(null)
  const [f, setF] = useState({ coinId: '', direction: 'ABOVE', targetPrice: '' })
  const [err, setErr] = useState({})
  const [busy, setBusy] = useState(false)

  const create = async (e) => {
    e.preventDefault()
    const errs = {}
    if (!f.coinId) errs.coinId = 'Choose a coin'
    if (!(Number(f.targetPrice) > 0)) errs.targetPrice = 'Enter a price greater than 0'
    setErr(errs)
    if (Object.keys(errs).length) return
    setBusy(true)
    try { await api.addAlert({ ...f, targetPrice: Number(f.targetPrice) }); notify('Alert created', 'success'); setOpen(false); setF({ coinId: '', direction: 'ABOVE', targetPrice: '' }); al.reload() }
    catch (ex) { setErr({ ...(ex.details || {}), form: ex.message }) } finally { setBusy(false) }
  }
  const remove = async () => {
    setBusy(true)
    try { await api.deleteAlert(deleting.id); notify('Alert deleted', 'success'); setDeleting(null); al.reload() }
    catch (ex) { notify(ex.message, 'error') } finally { setBusy(false) }
  }

  if (al.error && !al.data) return <ErrorBox message={al.error} onRetry={al.reload} />
  const coin = coins.data?.coins.find((c) => c.id === f.coinId)

  return (
    <>
      <PageHeader title="Price Alerts" subtitle="Checked every minute while this app is open">
        {al.data && <SourceBadge source={al.data.source} updatedAt={al.data.updatedAt} />}
        <button className="btn-primary" disabled={!coins.data} onClick={() => setOpen(true)}><Plus size={16} /> New alert</button>
      </PageHeader>
      <Notice tone="info" text="Alerts appear as in-app notifications only while the app is open in your browser. Background, email or push notifications would need extra infrastructure (a scheduler and a notification service), which this MVP does not include." />
      <Notice text={al.data?.notice} />
      {al.loading ? <SkeletonTable rows={3} /> : al.data.alerts.length === 0 ? <Empty title="No alerts" text="Create an alert to be notified when a coin crosses a price." /> : (
        <Card><ul className="divide-y divide-line">
          {al.data.alerts.map((a) => (
            <li key={a.id} className="flex flex-wrap items-center gap-3 py-3">
              <BellRing size={18} className={a.triggeredAt ? 'text-amber-300' : 'text-slate-500'} />
              <div className="min-w-0 flex-1">
                <p className="font-medium text-ink">{a.symbol} {a.direction === 'ABOVE' ? 'rises to or above' : 'falls to or below'} {money(a.targetPrice)}</p>
                <p className="text-xs text-slate-500">{a.triggeredAt ? `Triggered ${dateTime(a.triggeredAt)} at ${money(a.triggeredPrice)}` : `Created ${dateTime(a.createdAt)} · waiting`}</p>
              </div>
              <span className={`rounded px-2 py-0.5 text-xs font-semibold ${a.triggeredAt ? 'bg-amber-500/15 text-amber-300' : 'bg-slate-500/15 text-slate-300'}`}>{a.triggeredAt ? 'TRIGGERED' : 'ACTIVE'}</span>
              <button className="btn-ghost px-2 py-1 text-red-300" onClick={() => setDeleting(a)} aria-label={`Delete ${a.symbol} alert`}><Trash2 size={14} /></button>
            </li>
          ))}
        </ul></Card>
      )}
      {open && coins.data && (
        <Modal title="New price alert" onClose={() => setOpen(false)}>
          <form onSubmit={create} className="space-y-3" noValidate>
            <Field label="Coin" error={err.coinId}>
              <select className="input" value={f.coinId} onChange={(e) => setF({ ...f, coinId: e.target.value })}>
                <option value="">Select a coin…</option>
                {coins.data.coins.map((c) => <option key={c.id} value={c.id}>{c.name} ({c.symbol})</option>)}
              </select>
            </Field>
            <Field label="Condition">
              <select className="input" value={f.direction} onChange={(e) => setF({ ...f, direction: e.target.value })}>
                <option value="ABOVE">Price rises to or above</option><option value="BELOW">Price falls to or below</option>
              </select>
            </Field>
            <Field label="Target price (INR)" error={err.targetPrice} hint={coin?.price != null ? `Current price: ${money(coin.price)}` : undefined}>
              <input className="input" type="number" step="any" min="0" inputMode="decimal" value={f.targetPrice} onChange={(e) => setF({ ...f, targetPrice: e.target.value })} />
            </Field>
            {err.form && <p role="alert" className="rounded-lg bg-red-500/10 p-2 text-sm text-red-300">{err.form}</p>}
            <div className="flex justify-end gap-2 pt-2"><button type="button" className="btn-ghost" onClick={() => setOpen(false)}>Cancel</button><button className="btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Create alert'}</button></div>
          </form>
        </Modal>
      )}
      {deleting && <ConfirmDialog title="Delete alert?" text={`Delete the ${deleting.symbol} alert at ${money(deleting.targetPrice)}?`} busy={busy} onCancel={() => setDeleting(null)} onConfirm={remove} />}
    </>
  )
}
