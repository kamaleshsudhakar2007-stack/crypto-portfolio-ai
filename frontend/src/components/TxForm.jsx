import { useState } from 'react'
import { api } from '../api'
import { Field, Modal } from './ui'
import { toLocalInput } from '../lib/format'

/**
 * mode="holding": records an initial BUY (POST /api/holdings)
 * mode="tx": buy/sell, create or edit (POST/PUT /api/transactions)
 */
export default function TxForm({ mode = 'tx', initial, coins, presetCoinId, onClose, onSaved }) {
  const editing = !!initial
  const [f, setF] = useState({
    coinId: initial?.coinId || presetCoinId || '',
    type: initial?.type || 'BUY',
    quantity: initial?.quantity ?? '',
    price: initial?.price ?? '',
    fee: initial?.fee ?? '',
    timestamp: toLocalInput(initial ? new Date(initial.timestamp) : new Date()),
  })
  const [errors, setErrors] = useState({})
  const [formError, setFormError] = useState(null)
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })
  const coin = coins.find((c) => c.id === f.coinId)

  const submit = async (e) => {
    e.preventDefault()
    const errs = {}
    if (!f.coinId) errs.coinId = 'Choose a coin'
    if (!(Number(f.quantity) > 0)) errs.quantity = 'Enter a quantity greater than 0'
    if (!(Number(f.price) > 0)) errs.price = 'Enter a price greater than 0'
    if (f.fee !== '' && Number(f.fee) < 0) errs.fee = 'Fee cannot be negative'
    setErrors(errs)
    if (Object.keys(errs).length) return

    const body = {
      coinId: f.coinId,
      quantity: Number(f.quantity),
      price: Number(f.price),
      fee: f.fee === '' ? 0 : Number(f.fee),
      timestamp: new Date(f.timestamp).toISOString(),
    }
    setBusy(true); setFormError(null)
    try {
      if (mode === 'holding') await api.addHolding(body)
      else if (editing) await api.updateTransaction(initial.id, { ...body, type: f.type })
      else await api.addTransaction({ ...body, type: f.type })
      onSaved(editing ? 'Transaction updated' : mode === 'holding' ? 'Holding added' : 'Transaction recorded')
    } catch (err) {
      setErrors(err.details || {})
      setFormError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const title = editing ? 'Edit transaction' : mode === 'holding' ? 'Add holding' : 'Record transaction'
  return (
    <Modal title={title} onClose={onClose}>
      <form onSubmit={submit} className="space-y-3" noValidate>
        <Field label="Coin" error={errors.coinId}>
          <select className="input" value={f.coinId} onChange={set('coinId')} disabled={editing}>
            <option value="">Select a coin…</option>
            {coins.map((c) => <option key={c.id} value={c.id}>{c.name} ({c.symbol})</option>)}
          </select>
        </Field>
        {mode === 'tx' && (
          <Field label="Type">
            <select className="input" value={f.type} onChange={set('type')}>
              <option value="BUY">Buy</option>
              <option value="SELL">Sell</option>
            </select>
          </Field>
        )}
        <div className="grid grid-cols-2 gap-3">
          <Field label="Quantity" error={errors.quantity}>
            <input className="input" type="number" step="any" min="0" inputMode="decimal" value={f.quantity} onChange={set('quantity')} />
          </Field>
          <Field label="Price per coin (INR)" error={errors.price}>
            <input className="input" type="number" step="any" min="0" inputMode="decimal" value={f.price} onChange={set('price')} />
          </Field>
        </div>
        {coin?.price != null && (
          <button type="button" className="text-xs text-indigo-300 hover:underline" onClick={() => setF({ ...f, price: coin.price })}>
            Use current market price ({coin.price})
          </button>
        )}
        <div className="grid grid-cols-2 gap-3">
          <Field label="Fee (INR, optional)" error={errors.fee}>
            <input className="input" type="number" step="any" min="0" inputMode="decimal" value={f.fee} onChange={set('fee')} />
          </Field>
          <Field label="Date & time" error={errors.timestamp}>
            <input className="input" type="datetime-local" value={f.timestamp} onChange={set('timestamp')} />
          </Field>
        </div>
        {formError && <p role="alert" className="rounded-lg bg-red-500/10 p-2 text-sm text-red-300">{formError}</p>}
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" className="btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
        </div>
      </form>
    </Modal>
  )
}
