import { useCallback, useEffect, useRef, useState } from 'react'

/** Loads data on mount (and every `pollMs` if given). Keeps old data visible while refreshing. */
export function useApi(fn, { pollMs } = {}) {
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)
  const fnRef = useRef(fn)
  fnRef.current = fn

  const reload = useCallback(async () => {
    try {
      setData(await fnRef.current())
      setError(null)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    reload()
    if (!pollMs) return
    const t = setInterval(reload, pollMs)
    return () => clearInterval(t)
  }, [reload, pollMs])

  return { data, error, loading, reload }
}
