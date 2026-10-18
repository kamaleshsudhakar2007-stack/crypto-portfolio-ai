import { useMemo, useState } from 'react'
import { ArrowDown, ArrowUp, Search } from 'lucide-react'
import { api } from '../api'
import { useApi } from '../lib/useApi'
import { Card, Empty, ErrorBox, Notice, PageHeader, SkeletonTable, SourceBadge } from '../components/ui'
import { bigMoney, pct, price, tone } from '../lib/format'

const COLS = [
  { key: 'name', label: 'Coin' },
  { key: 'price', label: 'Price' },
  { key: 'change24h', label: '24h' },
  { key: 'marketCap', label: 'Market cap' },
]

export default function Markets() {
  const { data, error, loading, reload } = useApi(api.coins, { pollMs: 60000 })
  const [q, setQ] = useState('')
  const [sort, setSort] = useState({ key: 'marketCap', dir: 'desc' })

  const rows = useMemo(() => {
    if (!data) return []
    const t = q.trim().toLowerCase()
    const f = data.coins.filter((c) => !t || c.name.toLowerCase().includes(t) || c.symbol.toLowerCase().includes(t))
    const m = sort.dir === 'asc' ? 1 : -1
    return [...f].sort((a, b) => {
      const x = a[sort.key], y = b[sort.key]
      if (x == null) return 1
      if (y == null) return -1
      return typeof x === 'string' ? m * x.localeCompare(y) : m * (x - y)
    })
  }, [data, q, sort])

  const toggle = (key) => setSort((s) => (s.key === key ? { key, dir: s.dir === 'asc' ? 'desc' : 'asc' } : { key, dir: key === 'name' ? 'asc' : 'desc' }))

  if (error && !data) return <ErrorBox message={error} onRetry={reload} />
  return (
    <>
      <PageHeader title="Cryptocurrency Markets" subtitle="Top coins by market capitalisation (INR)">
        {data && <SourceBadge source={data.source} updatedAt={data.updatedAt} />}
      </PageHeader>
      <Notice text={data?.notice} />
      <div className="relative mb-4 max-w-sm">
        <Search size={16} className="absolute left-3 top-2.5 text-slate-500" />
        <input className="input pl-9" placeholder="Search name or symbol" aria-label="Search coins" value={q} onChange={(e) => setQ(e.target.value)} />
      </div>
      {loading ? <SkeletonTable rows={10} /> : rows.length === 0 ? <Empty title="No coins match your search" /> : (
        <Card>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[560px]">
              <thead><tr>
                <th className="th">#</th>
                {COLS.map((c) => (
                  <th key={c.key} className="th" aria-sort={sort.key === c.key ? (sort.dir === 'asc' ? 'ascending' : 'descending') : 'none'}>
                    <button className="inline-flex items-center gap-1 uppercase tracking-wide hover:text-ink" onClick={() => toggle(c.key)}>
                      {c.label}{sort.key === c.key && (sort.dir === 'asc' ? <ArrowUp size={12} /> : <ArrowDown size={12} />)}
                    </button>
                  </th>
                ))}
              </tr></thead>
              <tbody className="divide-y divide-line">
                {rows.map((c, i) => (
                  <tr key={c.id}>
                    <td className="td text-slate-500">{i + 1}</td>
                    <td className="td"><div className="flex items-center gap-2">
                      {c.image && <img src={c.image} alt="" className="h-6 w-6 rounded-full" loading="lazy" />}
                      <span className="font-medium text-ink">{c.name}</span><span className="text-xs text-slate-500">{c.symbol}</span>
                    </div></td>
                    <td className="td">{price(c.price)}</td>
                    <td className={`td ${tone(c.change24h)}`}>{pct(c.change24h)}</td>
                    <td className="td">{bigMoney(c.marketCap)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </>
  )
}
