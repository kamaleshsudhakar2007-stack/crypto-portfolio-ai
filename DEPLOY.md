# Deploy (free tiers: MongoDB Atlas + Render + Vercel)

Order matters: database -> backend -> frontend -> link CORS.

## 1. Database: MongoDB Atlas
1. Create a free (M0) cluster at mongodb.com/atlas, a database user, and under Network Access allow 0.0.0.0/0 (Render's IPs change).
2. Copy the connection string and add the db name:
   `mongodb+srv://USER:PASS@cluster.xxxxx.mongodb.net/crypto_portfolio?retryWrites=true&w=majority`

## 2. Backend: Render (Docker web service)
1. Push the project to GitHub (`.env` is git-ignored; never commit it).
2. Render -> New -> Web Service -> pick the repo, **Root Directory: backend**, Runtime: Docker.
3. Environment variables:
   | Key | Value |
   |---|---|
   | MONGODB_URI | your Atlas string |
   | COINGECKO_API_KEY | your demo key (optional) |
   | GEMINI_API_KEY | optional |
   | CORS_ORIGINS | your Vercel URL (set after step 3), e.g. https://crypto-portfolio-ai.vercel.app |
   | DEV_ENDPOINTS | false |
4. Health check path: `/api/health`. Deploy, then open `https://<service>.onrender.com/api/health`.

## 3. Frontend: Vercel
1. Vercel -> Add New Project -> same repo, **Root Directory: frontend**, framework Vite.
2. Environment variable: `VITE_API_URL` = `https://<service>.onrender.com` (no trailing slash).
3. Deploy, copy the URL, put it in Render's `CORS_ORIGINS`, and redeploy the backend.

## 4. Check
- Open the Vercel URL; the red "backend not reachable" banner must not show.
- Add a transaction, refresh, confirm it persists. Markets should show LIVE.

## Things to know
- **No login:** anyone with the URL sees and edits the same data. Fine for a demo; share the link carefully.
- Free Render services sleep when idle, so the first request can take about a minute.
- `DEV_ENDPOINTS=false` disables sample-data load, reset and the migration endpoint. Set it to `true` temporarily if you need them (first load of sample data, for example).
- Rotate any API key you ever pasted into chat or committed.
