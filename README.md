# Crypto Portfolio AI

College full-stack project: a dark fintech-style dashboard to track a cryptocurrency portfolio with live market data, transaction-based cost basis, analytics, price alerts and AI/rule-based insights.

**Stack:** React 18 + Vite + Tailwind CSS + Recharts + Lucide (frontend) · Java 17+ / Spring Boot 3.3 / Maven / Spring Data MongoDB / Bean Validation (backend) · MongoDB · CoinGecko public API · optional Google Gemini free tier.

> **Local demo app.** There is no login. Data is not isolated between users. No real trading, wallets or private keys.

## 1. Prerequisites (macOS)

```bash
brew install openjdk@17 maven node
brew tap mongodb/brew && brew install mongodb-community     # or use MongoDB Atlas free tier
brew services start mongodb-community
```
Add Java to your PATH if brew tells you to. Check: `java -version` (17+), `mvn -v`, `node -v` (18+), `mongosh --eval "db.runCommand({ping:1})"`.

## 2. Run

Terminal 1, backend:
```bash
cd backend
cp .env.example .env        # optional: edit keys / Atlas URI
mvn spring-boot:run         # http://localhost:8080
```
Terminal 2, frontend:
```bash
cd frontend
npm install
npm run dev                 # http://localhost:5173  (proxies /api to :8080)
```
Open http://localhost:5173 → **Settings → Load sample data**.

## 3. Environment variables (`backend/.env`)

| Variable | Required | Purpose |
|---|---|---|
| `MONGODB_URI` | no (defaults to local) | MongoDB connection string, e.g. Atlas `mongodb+srv://…` |
| `COINGECKO_API_KEY` | no | CoinGecko demo key for higher rate limits |
| `GEMINI_API_KEY` | no | Enables the optional AI summary on the AI Insights page |
| `GEMINI_MODEL` | no | Defaults to `gemini-2.5-flash`; change if Google renames/retires it |
| `CORS_ORIGINS` | no | Allowed frontend origins |

Keys are read only by the backend. Nothing secret is in the frontend. `.env` is git-ignored.

## 4. Architecture

```
React (Vite) ──/api──▶ Spring Boot controllers ──▶ services ──▶ MongoDB (transactions, alerts, snapshots)
                                                      └──────▶ CoinGecko (cached 60s, 30s back-off, stale → demo fallback)
                                                      └──────▶ Gemini (optional, server-side only)
```
- **Transactions are the source of truth.** Holdings are derived from them by `PortfolioCalculator` (pure Java, unit-tested).
- **Cost basis: weighted average.** Buy: `cost += qty×price + fee`. Sell: `costRemoved = avgCost×qty`, `realized += qty×price − fee − costRemoved`; average cost per unit is unchanged by a sale.
- **Unrealized** = current value − cost basis (open positions). **Realized** = from sales only. They are never mixed; return % is `null` when cost basis is 0.
- Transactions are processed chronologically, so selling more than owned *at that point in time* (including back-dated sells) is rejected; deleting a buy that a later sell depends on is blocked (409).
- All money uses `BigDecimal`.
- **Data source labels:** every market-dependent screen shows LIVE / STALE / DEMO with a timestamp. Demo values are static samples and are never used to trigger alerts or record snapshots.
- **History:** portfolio-value snapshots are saved from live prices (max every 15 min) while the app is used. The performance chart appears once ≥2 snapshots exist; nothing is invented.
- **Alerts** are evaluated when the list is fetched (UI polls every 60 s). In-app only; background/email/push would need a scheduler and notification service.
- **Insights:** rule-based rules are always shown and labelled "not AI". The AI summary appears only when a key is set *and* you click the button, in a separate labelled card.
- Errors always use `{timestamp,status,error,message,details}`.

See `docs/API.md` for endpoints.

## 5. Testing

### Actually executed while building
- Frontend: `npm install && npm run build` succeeded.
- Cost-basis logic (`PortfolioCalculator`, `Transaction`, `ApiException`) compiled with `javac` and run against hand-computed cases: weighted average with fees, realized P/L on a partial sell, full close-out, oversell rejected, back-dated sell rejected. All passed.

### NOT executed (no Maven Central / MongoDB / browser in the build environment). Please run:
1. `cd backend && mvn test` (runs `PortfolioCalculatorTest`) then `mvn spring-boot:run`.
2. `curl localhost:8080/api/health` → `{"status":"UP","database":"UP",…}`
3. `curl localhost:8080/api/coins | head -c 400` → `"source":"live"`. Turn Wi-Fi off and restart to see the DEMO fallback.
4. Add a holding:
   ```bash
   curl -X POST localhost:8080/api/holdings -H 'Content-Type: application/json' \
     -d '{"coinId":"bitcoin","quantity":0.1,"price":50000,"fee":5}'
   curl localhost:8080/api/portfolio/summary
   ```
5. Oversell is rejected (expect 400):
   ```bash
   curl -X POST localhost:8080/api/transactions -H 'Content-Type: application/json' \
     -d '{"coinId":"bitcoin","type":"SELL","quantity":99,"price":60000}'
   ```
6. Invalid input (expect 400 with `details`): `-d '{"coinId":"bitcoin","quantity":-1,"price":0}'` to `/api/holdings`.
7. Persistence: add data, refresh the browser, restart the backend; data is still there.
8. UI checklist: empty states (Settings → Delete all data), loading skeletons, backend-down banner (stop the backend), mobile layout (browser device toolbar, bottom nav), keyboard Tab/Esc in dialogs.
9. Alerts: create "BTC above <price just under current>", wait ≤60 s → toast + TRIGGERED badge.

## 6. Demo script for faculty (~5 min)
1. Settings → show system status, load sample data.
2. Dashboard → totals, unrealized vs realized, allocation, LIVE badge + timestamp.
3. Portfolio → average cost per coin; add a purchase via the + button.
4. Transactions → record a SELL larger than holdings to show validation; delete confirmation.
5. Analytics → best/worst, P/L by asset, honest "not enough history" note.
6. Alerts → create one just above/below current price.
7. AI Insights → rule-based (labelled); AI summary if key set.
8. Mention architecture + known limitations.

## 7. Screenshots to capture
Dashboard · My Portfolio · Markets (with LIVE badge) · Add-holding dialog with validation error · Transactions with filter · Analytics · Alerts with a triggered alert · AI Insights · Mobile view · Terminal showing `/api/health` · MongoDB Compass showing the `transactions` collection.

## 8. Known limitations / future work
- No authentication or multi-user isolation.
- CoinGecko free tier is rate-limited; only the top 50 coins by market cap are supported.
- Sell-fee and buy-fee conventions are simple; no tax lots (FIFO/LIFO), staking, transfers or multi-currency.
- Alerts need the app open; no scheduler/email/push.
- Performance history only accumulates from when the app is used.
- Gemini model name/endpoint not verified in the build environment.
- Future: JWT auth, scheduled snapshots, WebSocket prices, CSV import/export, backend integration tests with Testcontainers.


## 5. Currency: INR (₹)

- **No runtime conversion.** CoinGecko is queried with `vs_currency=inr`, so prices, market caps, portfolio values and P&L are INR end to end. The frontend only formats (`Intl.NumberFormat('en-IN')`, e.g. ₹1,25,000.00).
- Transactions, fees and alert targets you enter are interpreted as INR.
- **Existing USD data:** records created before this version are USD. Convert them once (nothing is written by default):
  ```bash
  curl -X POST "http://localhost:8080/api/dev/migrate-to-inr?dryRun=true"    # preview
  curl -X POST "http://localhost:8080/api/dev/migrate-to-inr?dryRun=false"   # apply
  ```
  Transactions/snapshots use the USD→INR rate of their own date (Frankfurter, ECB reference rates); alerts use today's rate. Originals are copied to `transactions_usd_backup`, `alerts_usd_backup`, `snapshots_usd_backup`; converted records carry `fxConverted`/`fxRate`, so it cannot run twice. If the rate API is down, nothing is changed. Until you run it, old USD records would be mixed with INR prices, so run it before trusting portfolio numbers. (Alternatively: Settings → reset, then load sample data.)
