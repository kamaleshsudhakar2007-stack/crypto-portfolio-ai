import { useEffect, useRef } from 'react'
import { AlertTriangle, Inbox, X } from 'lucide-react'
import { dateTime } from '../lib/format'

export const Card = ({ title, action, children, className = '' }) => (
  <section className={`glass rounded-3xl p-5 ${className}`}>
    {(title || action) && (
      <div className="mb-3 flex items-center justify-between gap-2">
        <h2 className="text-sm font-semibold text-slate-300">{title}</h2>
        {action}
      </div>
    )}
    {children}
  </section>
)

export const PageHeader = ({ title, subtitle, children }) => (
  <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
    <div>
      <h1 className="text-xl font-bold text-ink">{title}</h1>
      {subtitle && <p className="mt-0.5 text-sm text-slate-400">{subtitle}</p>}
    </div>
    <div className="flex flex-wrap items-center gap-2">{children}</div>
  </div>
)

export const Stat = ({ label, value, sub, valueClass = 'text-ink' }) => (
  <Card>
    <p className="text-xs font-medium uppercase tracking-wide text-slate-400">{label}</p>
    <p className={`mt-1 text-2xl font-bold ${valueClass}`}>{value}</p>
    {sub && <p className="mt-1 text-xs text-slate-500">{sub}</p>}
  </Card>
)

export const Skeleton = ({ className = 'h-6 w-full' }) => <div className={`skeleton ${className}`} aria-hidden="true" />

export const SkeletonCards = ({ n = 4 }) => (
  <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-busy="true" aria-label="Loading">
    {Array.from({ length: n }).map((_, i) => (
      <Card key={i}><Skeleton className="h-3 w-24" /><Skeleton className="mt-3 h-8 w-32" /></Card>
    ))}
  </div>
)

export const SkeletonTable = ({ rows = 6 }) => (
  <Card><div className="space-y-3" aria-busy="true" aria-label="Loading">
    {Array.from({ length: rows }).map((_, i) => <Skeleton key={i} className="h-8 w-full" />)}
  </div></Card>
)

export const ErrorBox = ({ message, onRetry }) => (
  <div role="alert" className="flex flex-wrap items-center gap-3 rounded-2xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300">
    <AlertTriangle size={18} className="shrink-0" />
    <span className="flex-1">{message}</span>
    {onRetry && <button className="btn-ghost" onClick={onRetry}>Retry</button>}
  </div>
)

export const Empty = ({ title, text, action }) => (
  <div className="flex flex-col items-center gap-2 rounded-3xl border border-dashed border-slate-500/30 bg-white/40 p-10 text-center">
    <Inbox className="text-slate-500" size={32} />
    <p className="font-medium text-slate-200">{title}</p>
    {text && <p className="max-w-md text-sm text-slate-400">{text}</p>}
    {action}
  </div>
)

/** Labels where the numbers come from: live, last-known (stale) or sample (demo). */
export function SourceBadge({ source, updatedAt }) {
  const map = {
    live: ['bg-emerald-500/15 text-emerald-300', 'LIVE'],
    stale: ['bg-amber-500/15 text-amber-300', 'STALE (last live data)'],
    demo: ['bg-red-500/15 text-red-300', 'DEMO DATA'],
  }
  const [cls, label] = map[source] || ['bg-slate-500/15 text-slate-300', '—']
  return (
    <span className="inline-flex items-center gap-2 text-xs text-slate-400">
      <span className={`rounded px-2 py-0.5 font-semibold ${cls}`}>{label}</span>
      {updatedAt && <span>Updated {dateTime(updatedAt)}</span>}
    </span>
  )
}

export const Notice = ({ text, tone = 'warn' }) =>
  text ? (
    <div className={`mb-4 rounded-lg border p-3 text-sm ${tone === 'warn' ? 'border-amber-500/40 bg-amber-500/10 text-amber-200' : 'border-sky-500/40 bg-sky-500/10 text-sky-200'}`}>
      {text}
    </div>
  ) : null

export function Modal({ title, onClose, children }) {
  const ref = useRef(null)
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', onKey)
    ref.current?.querySelector('input,select,button')?.focus()
    return () => document.removeEventListener('keydown', onKey)
  }, [onClose])
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-slate-900/25 backdrop-blur-sm p-0 sm:items-center sm:p-4" onMouseDown={onClose}>
      <div ref={ref} role="dialog" aria-modal="true" aria-label={title}
        className="max-h-[92vh] w-full max-w-lg overflow-y-auto glass glass-strong rounded-t-3xl p-6 sm:rounded-3xl"
        onMouseDown={(e) => e.stopPropagation()}>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-ink">{title}</h2>
          <button className="rounded p-1 text-slate-400 hover:bg-black/[0.06]" onClick={onClose} aria-label="Close"><X size={18} /></button>
        </div>
        {children}
      </div>
    </div>
  )
}

export function ConfirmDialog({ title, text, confirmLabel = 'Delete', busy, onConfirm, onCancel }) {
  return (
    <Modal title={title} onClose={onCancel}>
      <p className="text-sm text-slate-300">{text}</p>
      <div className="mt-5 flex justify-end gap-2">
        <button className="btn-ghost" onClick={onCancel}>Cancel</button>
        <button className="btn-danger" disabled={busy} onClick={onConfirm}>{busy ? 'Working…' : confirmLabel}</button>
      </div>
    </Modal>
  )
}

export const Field = ({ label, error, children, hint }) => (
  <label className="block">
    <span className="mb-1 block text-xs font-medium text-slate-400">{label}</span>
    {children}
    {hint && !error && <span className="mt-1 block text-xs text-slate-500">{hint}</span>}
    {error && <span className="mt-1 block text-xs text-red-400">{error}</span>}
  </label>
)
