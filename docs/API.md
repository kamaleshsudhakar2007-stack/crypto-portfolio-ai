# REST API (base `http://localhost:8080/api`)

Errors: `{ "timestamp", "status", "error", "message", "details": { field: message } }`

| Method | Path | Description |
|---|---|---|
| GET | `/health` | `{status, database, aiConfigured, time}` (200 even if DB is down; `database:"DOWN"`) |
| GET | `/coins` | `{coins[], source: live\|stale\|demo, updatedAt, notice}` |
| GET | `/coins/{coinId}` | One coin or 404 |
| GET | `/portfolio` | `{holdings[], summary}` derived from transactions |
| GET | `/portfolio/summary` | totalValue, totalInvested, unrealizedPnl, returnPct, realizedPnl, source, updatedAt, notice |
| POST | `/holdings` | Body `{coinId, quantity, price, fee?, timestamp?}`; records an initial BUY; 201 |
| DELETE | `/holdings/{coinId}` | Deletes the whole position (all its transactions); 204 |
| GET | `/transactions` | All, newest first |
| POST | `/transactions` | Body `{coinId, type: BUY\|SELL, quantity, price, fee?, timestamp?}`; 201; 400 if it would oversell |
| PUT | `/transactions/{id}` | Same body; revalidates full history |
| DELETE | `/transactions/{id}` | 204; 409 if a later sell depends on it |
| GET | `/analytics` | allocation, pnlByAsset, best, worst, concentration, performance snapshots (+note) |
| GET | `/alerts` | Evaluates against live prices, returns `{alerts[], source, notice}` |
| POST | `/alerts` | Body `{coinId, direction: ABOVE\|BELOW, targetPrice}`; 201 |
| DELETE | `/alerts/{id}` | 204 |
| GET | `/insights?ai=false` | Rule-based insights; `ai=true` adds an AI summary if a key is configured |
| POST | `/dev/seed` | Loads 5 sample transactions if empty (disable with `app.dev-endpoints=false`) |
| DELETE | `/dev/reset` | Deletes all transactions, alerts, snapshots |

There is no `PUT /holdings`: a holding is derived data, so edit the underlying purchase with `PUT /transactions/{id}`.
