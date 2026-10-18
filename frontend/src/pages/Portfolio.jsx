import { useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { api } from '../api'
import { useApi } from '../lib/useApi'
import { Card, ConfirmDialog, Empty, ErrorBox, Notice, PageHeader, SkeletonTable, SourceBadge } from '../components/ui'
import TxForm from '../components/TxForm'
import { useNotify } from '../components/Notify'
import { money, pct, price, qty, signedMoney, tone } from '../lib/format'

export default function Portfolio() {
  const notify = useNotify()
  const pf = useApi(api.portfolio, { pollMs: 60000 })
  const coins = useApi(api.coins)
  const [adding, setAdding] = useState(null) // null | {coinId?}
  const [deleting, setDeleting] = useState(null)
  const [busy, setBusy] = useState(false)

  const remove = async () => {
    setBusy(true)
    try { await api.deleteHolding(deleting.coinId); notify(`${deleting.symbol} holding removed`, 'success'); setDeleting(null); pf.reload() }
    catch (e) { notify(e.message, 'error') } finally { setBusy(false) }
  }

  if (pf.error && !pf.data) return <ErrorBox message={pf.error} onRetry={pf.reload} />
  const s = pf.data?.summary

  return (
    <>
      <PageHeader title="My Portfolio" subtitle="Holdings are calculated from your transactions (weighted-average cost)">
        {s && <SourceBadge source={s.source} updatedAt={s.updatedAt} />}
        <button className="btn-primary" disabled={!coins.data} onClick={() => setAdding({})}><Plus size={16} /> Add holding</button>
      </PageHeader>
      <Notice text={s?.notice} />
      {pf.loading ? <SkeletonTable /> : pf.data.holdings.length === 0 ? (
        <Empty title="No holdings yet" text="Add a holding to record your first purchase." />
      ) : (
        <Card>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px]">
              <thead><tr>
                <th className="th">Asset</th><th className="th">Quantity</th><th className="th">Avg cost</th><th className="th">Cost basis</th>
                <th className="th">Price</th><th className="th">Value</th><th className="th">Unrealized P/L</th><th className="th">Realized P/L</th><th className="th"><span className="sr-only">Actions</span></th>
              </tr></thead>
              <tbody className="divide-y divide-line">
                {pf.data.holdings.map((h) => (
                  <tr key={h.coinId}>
                    <td className="td"><p className="font-medium text-ink">{h.symbol}</p><p className="text-xs text-slate-500">{h.name}</p></td>
                    <td className="td">{qty(h.quantity)}</td>
                    <td className="td">{price(h.avgCost)}</td>
                    <td className="td">{money(h.costBasis)}</td>
                    <td className="td">{h.priceAvailable ? price(h.currentPrice) : <span className="text-amber-300">No price</span>}</td>
                    <td className="td">{money(h.currentValue)}</td>
                    <td className={`td ${tone(h.unrealizedPnl)}`}>{signedMoney(h.unrealizedPnl)}<br /><span className="text-xs">{pct(h.returnPct)}</span></td>
                    <td className={`td ${tone(h.realizedPnl)}`}>{signedMoney(h.realizedPnl)}</td>
                    <td className="td">
                      <div className="flex justify-end gap-1">
                        <button className="btn-ghost px-2 py-1" onClick={() => setAdding({ coinId: h.coinId })} aria-label={`Buy more ${h.symbol}`}><Plus size={14} /></button>
                        <button className="btn-ghost px-2 py-1 text-red-300" onClick={() => setDeleting(h)} aria-label={`Delete ${h.symbol} holding`}><Trash2 size={14} /></button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="mt-3 text-xs text-slate-500">
            Unrealized P/L = current value − cost basis (open positions). Realized P/L comes from sales, after fees. To edit a purchase, change it on the Transactions page.
          </p>
        </Card>
      )}
      {adding && coins.data && (
        <TxForm mode="holding" coins={coins.data.coins} presetCoinId={adding.coinId} onClose={() => setAdding(null)}
          onSaved={(m) => { setAdding(null); notify(m, 'success'); pf.reload() }} />
      )}
      {deleting && (
        <ConfirmDialog title={`Delete ${deleting.symbol} holding?`} busy={busy} onCancel={() => setDeleting(null)} onConfirm={remove}
          text={`This permanently deletes the position and all ${deleting.symbol} transactions, including past sales and their realized P/L. This cannot be undone.`} />
      )}
    </>
  )
}
