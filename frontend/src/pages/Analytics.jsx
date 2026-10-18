import { Bar, BarChart, CartesianGrid, Cell, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '../api'
import { useApi } from '../lib/useApi'
import { Card, Empty, ErrorBox, Notice, PageHeader, SkeletonCards, SkeletonTable, SourceBadge, Stat } from '../components/ui'
import { COLORS } from './Dashboard'
import { money, pct, tone } from '../lib/format'

const tip = { background: 'rgba(255,255,255,0.92)', border: '1px solid #222c44', borderRadius: 8 }

export default function Analytics() {
  const { data, error, loading, reload } = useApi(api.analytics, { pollMs: 60000 })
  if (error && !data) return <ErrorBox message={error} onRetry={reload} />
  if (loading) return <><PageHeader title="Analytics" /><SkeletonCards n={3} /><div className="mt-4"><SkeletonTable rows={4} /></div></>
  if (data.pnlByAsset.length === 0) return <><PageHeader title="Analytics" /><Empty title="Nothing to analyse yet" text="Add holdings or transactions first." /></>

  const pnl = data.pnlByAsset.map((a) => ({ ...a, unrealizedPnl: a.unrealizedPnl ?? 0 }))
  const perf = data.performance.map((p) => ({ t: new Date(p.timestamp).toLocaleString(), value: p.totalValue, invested: p.totalInvested }))

  return (
    <>
      <PageHeader title="Analytics" subtitle="Based on your transactions and recorded price snapshots">
        <SourceBadge source={data.source} updatedAt={data.updatedAt} />
      </PageHeader>
      {data.source !== 'live' && <Notice text={`Prices are ${data.source === 'demo' ? 'DEMO sample values' : 'stale'}, so figures below are not current market values.`} />}

      <div className="grid gap-4 sm:grid-cols-3">
        <Stat label="Best performer" value={data.best ? data.best.symbol : '—'} sub={data.best ? pct(data.best.returnPct) : 'No priced holdings'} valueClass="text-emerald-400" />
        <Stat label="Worst performer" value={data.worst ? data.worst.symbol : '—'} sub={data.worst ? pct(data.worst.returnPct) : 'No priced holdings'} valueClass={data.worst ? tone(data.worst.returnPct) : ''} />
        <Stat label="Largest position" value={data.topHoldingSymbol || '—'} sub={data.topHoldingPct != null ? `${Number(data.topHoldingPct).toFixed(1)}% of portfolio` : ''} />
      </div>

      <div className="mt-4 grid gap-4 lg:grid-cols-2">
        <Card title="Asset allocation">
          <div className="h-64" role="img" aria-label="Allocation by asset">
            <ResponsiveContainer><PieChart>
              <Pie data={data.allocation} dataKey="value" nameKey="symbol" outerRadius={90} stroke="none" label={(e) => e.symbol}>
                {data.allocation.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
              </Pie>
              <Tooltip formatter={(v) => money(v)} contentStyle={tip} />
            </PieChart></ResponsiveContainer>
          </div>
        </Card>
        <Card title="Unrealized profit / loss by asset (INR)">
          <div className="h-64" role="img" aria-label="Profit and loss by asset">
            <ResponsiveContainer><BarChart data={pnl}>
              <CartesianGrid stroke="rgba(20,22,28,0.09)" vertical={false} />
              <XAxis dataKey="symbol" stroke="#8a91a1" /><YAxis stroke="#8a91a1" width={60} />
              <Tooltip formatter={(v) => money(v)} contentStyle={tip} cursor={{ fill: 'rgba(20,22,28,0.04)' }} />
              <Bar dataKey="unrealizedPnl" name="Unrealized P/L" radius={[4, 4, 0, 0]}>
                {pnl.map((a, i) => <Cell key={i} fill={a.unrealizedPnl >= 0 ? '#10b981' : '#ef4444'} />)}
              </Bar>
            </BarChart></ResponsiveContainer>
          </div>
        </Card>
      </div>

      <Card title="Realized vs unrealized by asset" className="mt-4">
        <div className="overflow-x-auto"><table className="w-full min-w-[480px]">
          <thead><tr><th className="th">Asset</th><th className="th">Unrealized</th><th className="th">Return</th><th className="th">Realized (sales)</th></tr></thead>
          <tbody className="divide-y divide-line">
            {data.pnlByAsset.map((a) => (
              <tr key={a.symbol}><td className="td font-medium text-ink">{a.symbol}</td>
                <td className={`td ${tone(a.unrealizedPnl)}`}>{money(a.unrealizedPnl)}</td>
                <td className={`td ${tone(a.returnPct)}`}>{pct(a.returnPct)}</td>
                <td className={`td ${tone(a.realizedPnl)}`}>{money(a.realizedPnl)}</td></tr>
            ))}
          </tbody></table></div>
      </Card>

      <Card title="Portfolio value over time (recorded snapshots)" className="mt-4">
        {perf.length < 2 ? <p className="text-sm text-slate-400">{data.performanceNote}</p> : (
          <div className="h-72" role="img" aria-label="Portfolio value over time">
            <ResponsiveContainer><LineChart data={perf}>
              <CartesianGrid stroke="rgba(20,22,28,0.09)" vertical={false} />
              <XAxis dataKey="t" stroke="#8a91a1" tick={false} /><YAxis stroke="#8a91a1" width={70} domain={['auto', 'auto']} />
              <Tooltip formatter={(v) => money(v)} contentStyle={tip} />
              <Line type="monotone" dataKey="value" name="Value" stroke="#0a84ff" dot={false} strokeWidth={2} />
              <Line type="monotone" dataKey="invested" name="Invested" stroke="#8a91a1" dot={false} strokeDasharray="4 4" />
            </LineChart></ResponsiveContainer>
          </div>
        )}
      </Card>
    </>
  )
}
