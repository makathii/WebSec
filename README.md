# WebSecGram

A small Instagram clone for the web security course. Spring Boot backend, Angular frontend, PostgreSQL.
See `PLAN.md` for the design and the security measures.

## Run

```bash
cp .env.example .env
# fill in the secrets, e.g. with: openssl rand -base64 48
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080

If the app is accessed through another host than `localhost`, set `FRONTEND_ORIGIN` and `API_ORIGIN` in `.env`
(and `COOKIE_SECURE=false` when that host is plain http).

## Development

```bash
cd frontend && npm install && npm start   # http://localhost:4200, set FRONTEND_ORIGIN accordingly
```
