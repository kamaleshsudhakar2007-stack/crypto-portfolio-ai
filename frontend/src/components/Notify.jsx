import { createContext, useCallback, useContext, useState } from 'react'
import { BellRing, CheckCircle2, XCircle } from 'lucide-react'

const Ctx = createContext(() => {})
export const useNotify = () => useContext(Ctx)

export function NotifyProvider({ children }) {
  const [items, setItems] = useState([])
  const notify = useCallback((text, kind = 'info') => {
    const id = Math.random().toString(36).slice(2)
    setItems((l) => [...l, { id, text, kind }])
    setTimeout(() => setItems((l) => l.filter((x) => x.id !== id)), 7000)
  }, [])
  const Icon = { info: BellRing, success: CheckCircle2, error: XCircle }
  return (
    <Ctx.Provider value={notify}>
      {children}
      <div className="pointer-events-none fixed bottom-20 right-4 z-[60] flex w-80 max-w-[calc(100vw-2rem)] flex-col gap-2 md:bottom-4" aria-live="polite">
        {items.map((t) => {
          const I = Icon[t.kind]
          return (
            <div key={t.id} className="pointer-events-auto flex items-start gap-2 glass glass-strong rounded-2xl p-3 text-sm">
              <I size={18} className={t.kind === 'error' ? 'text-red-400' : t.kind === 'success' ? 'text-emerald-400' : 'text-amber-300'} />
              <span>{t.text}</span>
            </div>
          )
        })}
      </div>
    </Ctx.Provider>
  )
}
