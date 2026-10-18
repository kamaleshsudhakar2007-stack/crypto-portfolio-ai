import { useMemo, useState } from 'react'
import { Pencil, Plus, Search, Trash2 } from 'lucide-react'
import { api } from '../api'
import { useApi } from '../lib/useApi'
import { Card, ConfirmDialog, Empty, ErrorBox, PageHeader, SkeletonTable } from '../components/ui'
import TxForm from '../components/TxForm'
import { useNotify } from '../components/Notify'
import { dateTime, money, price, qty } from '../lib/format'

export default function Transactions() {
  const notify = useNotify()
  const tx = useApi(api.transactions)
  const coins = useApi(api.coins)
  const [q, setQ] = useState('')
  const [type, setType] = useState('ALL')
  const [form, setForm] = useState(null) // {} for new, tx for edit
  const [deleting, setDeleting] = useState(null)
  const [busy, setBusy] = useState(false)

  const rows = useMemo(() => {
    const t = q.trim().toLowerCase()
    return (tx.data || []).filter((r) => (type === 'ALL' || r.type === type) && (!t || r.symbol.toLowerCase().includes(t) || r.name.toLowerCase().includes(t)))
  }, [tx.data, q, type])

  const remove = async () => {
    setBusy(true)
    try { await api.deleteTransaction(deleting.id); notify('Transaction deleted', 'success'); setDeleting(null); tx.reload() }
    catch (e) { notify(e.message, 'error'); setDeleting(null) } finally { setBusy(false) }
  }

  if (tx.error && !tx.data) return <ErrorBox message={tx.error} onRetry={tx.reload} />
  return (
    <>
      <PageHeader title="Transactions" subtitle="Every buy and sell. Past records never change when prices move.">
        <button className="btn-primary" disabled={!coins.data} onClick={() => setForm({})}><Plus size={16} /> Record transaction</button>
      </PageHeader>
      <div className="mb-4 flex flex-wrap gap-3">
        <div className="relative w-full max-w-xs">
          <Search size={16} className="absolute left-3 top-2.5 text-slate-500" />
          <input className="input pl-9" placeholder="Filter by coin" aria-label="Filter by coin" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
        <select className="input w-auto" aria-label="Filter by type" value={type} onChange={(e) => setType(e.target.value)}>
          <option value="ALL">All types</option><option value="BUY">Buys</option><option value="SELL">Sells</option>
        </select>
      </div>
      {tx.loading ? <SkeletonTable /> : (tx.data || []).length === 0 ? (
        <Empty title="No transactions yet" text="Record a buy to start tracking your portfolio." />
      ) : rows.length === 0 ? <Empty title="No transactions match the filters" /> : (
        <Card>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px]">
              <thead><tr><th className="th">Date</th><th className="th">Type</th><th className="th">Asset</th><th className="th">Quantity</th><th className="th">Price</th><th className="th">Fee</th><th className="th">Total</th><th className="th"><span className="sr-only">Actions</span></th></tr></thead>
              <tbody className="divide-y divide-line">
                {rows.map((r) => {
                  const gross = r.quantity * r.price
                  const total = r.type === 'BUY' ? gross + (r.fee || 0) : gross - (r.fee || 0)
                  return (
                    <tr key={r.id}>
                      <td className="td whitespace-nowrap">{dateTime(r.timestamp)}</td>
                      <td className="td"><span className={`rounded px-2 py-0.5 text-xs font-semibold ${r.type === 'BUY' ? 'bg-emerald-500/15 text-emerald-300' : 'bg-red-500/15 text-red-300'}`}>{r.type}</span></td>
                      <td className="td font-medium text-ink">{r.symbol}</td>
                      <td className="td">{qty(r.quantity)}</td>
                      <td className="td">{price(r.price)}</td>
                      <td className="td">{money(r.fee)}</td>
                      <td className="td">{money(total)}</td>
                      <td className="td"><div className="flex justify-end gap-1">
                        <button className="btn-ghost px-2 py-1" onClick={() => setForm(r)} aria-label="Edit transaction"><Pencil size={14} /></button>
                        <button className="btn-ghost px-2 py-1 text-red-300" onClick={() => setDeleting(r)} aria-label="Delete transaction"><Trash2 size={14} /></button>
                      </div></td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </Card>
      )}
      {form && coins.data && (
        <TxForm mode="tx" initial={form.id ? form : undefined} coins={coins.data.coins} onClose={() => setForm(null)}
          onSaved={(m) => { setForm(null); notify(m, 'success'); tx.reload() }} />
      )}
      {deleting && (
        <ConfirmDialog title="Delete transaction?" busy={busy} onCancel={() => setDeleting(null)} onConfirm={remove}
          text={`Delete the ${deleting.type} of ${qty(deleting.quantity)} ${deleting.symbol}? Holdings and profit/loss will be recalculated. Deleting a buy that a later sell depends on is blocked.`} />
      )}
    </>
  )
}
