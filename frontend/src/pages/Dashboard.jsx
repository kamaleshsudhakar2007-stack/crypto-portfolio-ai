import { Link } from 'react-router-dom'
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import { api } from '../api'
import { useApi } from '../lib/useApi'
import { Card, Empty, ErrorBox, Notice, PageHeader, SkeletonCards, SkeletonTable, SourceBadge, Stat } from '../components/ui'
import { money, pct, price, signedMoney, tone } from '../lib/format'

export const COLORS = ['#0a84ff', '#5ac8fa', '#f59e0b', '#10b981', '#ec4899', '#a78bfa', '#f97316', '#84cc16']

export default function Dashboard() {
  const pf = useApi(api.portfolio, { pollMs: 60000 })
  const coins = useApi(api.coins, { pollMs: 60000 })

  if (pf.error && !pf.data) return <ErrorBox message={pf.error} onRetry={pf.reload} />
  if (pf.loading) return <><PageHeader title="Dashboard" /><SkeletonCards /><div className="mt-4"><SkeletonTable rows={4} /></div></>

  const { holdings, summary: s } = pf.data
  const priced = holdings.filter((h) => h.priceAvailable)

  return (
    <>
      <PageHeader title="Dashboard" subtitle="Overview of your portfolio">
        <SourceBadge source={s.source} updatedAt={s.updatedAt} />
      </PageHeader>
      <Notice text={s.notice} />

      {holdings.length === 0 ? (
        <Empty title="Your portfolio is empty" text="Add your first holding, or load sample data from Settings to explore the app."
          action={<Link className="btn-primary" to="/portfolio">Go to My Portfolio</Link>} />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <Stat label="Total value" value={money(s.totalValue)} />
            <Stat label="Total invested" value={money(s.totalInvested)} sub="Cost basis incl. fees" />
            <Stat label="Unrealized P/L" value={signedMoney(s.unrealizedPnl)} valueClass={tone(s.unrealizedPnl)} sub="Open positions only" />
            <Stat label="Return" value={pct(s.returnPct)} valueClass={tone(s.returnPct)} sub={`Realized P/L (closed sales): ${signedMoney(s.realizedPnl)}`} />
          </div>

          <div className="mt-4 grid gap-4 lg:grid-cols-5">
            <Card title="Allocation" className="lg:col-span-2">
              {priced.length === 0 ? <p className="text-sm text-slate-400">No priced holdings.</p> : (
                <div className="h-64" role="img" aria-label="Portfolio allocation pie chart">
                  <ResponsiveContainer>
                    <PieChart>
                      <Pie data={priced} dataKey="currentValue" nameKey="symbol" innerRadius={55} outerRadius={90} paddingAngle={2} stroke="none">
                        {priced.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                      </Pie>
                      <Tooltip formatter={(v) => money(v)} contentStyle={{ background: 'rgba(255,255,255,0.92)', border: '1px solid #222c44', borderRadius: 8 }} />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              )}
              <ul className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-400">
                {priced.map((h, i) => (
                  <li key={h.coinId} className="flex items-center gap-1"><span className="h-2 w-2 rounded-full" style={{ background: COLORS[i % COLORS.length] }} />{h.symbol} {Number(h.allocationPct).toFixed(1)}%</li>
                ))}
              </ul>
            </Card>

            <Card title="Top holdings" className="lg:col-span-3" action={<Link to="/portfolio" className="text-xs text-indigo-300 hover:underline">View all</Link>}>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[420px]">
                  <thead><tr><th className="th">Asset</th><th className="th">Value</th><th className="th">P/L</th></tr></thead>
                  <tbody className="divide-y divide-line">
                    {holdings.slice(0, 5).map((h) => (
                      <tr key={h.coinId}>
                        <td className="td font-medium text-ink">{h.symbol}</td>
                        <td className="td">{money(h.currentValue)}</td>
                        <td className={`td ${tone(h.unrealizedPnl)}`}>{signedMoney(h.unrealizedPnl)} <span className="text-xs">({pct(h.returnPct)})</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          </div>
        </>
      )}

      <Card title="Market overview" className="mt-4" action={<Link to="/markets" className="text-xs text-indigo-300 hover:underline">All markets</Link>}>
        {coins.loading ? <SkeletonTable rows={3} /> : coins.error ? <ErrorBox message={coins.error} onRetry={coins.reload} /> : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
            {coins.data.coins.slice(0, 6).map((c) => (
              <div key={c.id} className="rounded-lg border border-line p-3">
                <p className="text-xs text-slate-400">{c.symbol}</p>
                <p className="font-semibold text-ink">{price(c.price)}</p>
                <p className={`text-xs ${tone(c.change24h)}`}>{pct(c.change24h)}</p>
              </div>
            ))}
          </div>
        )}
      </Card>
    </>
  )
}
