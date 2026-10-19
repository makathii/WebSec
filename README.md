# WebSecGram

A small Instagram clone for the web security course. Spring Boot backend, Angular frontend, PostgreSQL.

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

## Captcha

After 3 failed logins from one IP (or 5 on one account) the login asks for a
[Cloudflare Turnstile](https://developers.cloudflare.com/turnstile/) captcha. The backend checks the token with
Cloudflare's siteverify API.

Without configuration Cloudflare's [test keys](https://developers.cloudflare.com/turnstile/troubleshooting/testing/)
are used: the widget always asks for the "Verify you are human" click and every token passes. For real checks create
a free Turnstile widget in the Cloudflare dashboard (no domain on Cloudflare needed, add `localhost` as hostname) and
set `TURNSTILE_SITE_KEY` and `TURNSTILE_SECRET_KEY` in `.env`. Other test keys: site key `1x00000000000000000000AA`
passes without a click, secret `2x0000000000000000000000000000000AA` always fails.

## Development

Requires Node.js 20.19+ or 22.12+ (Angular 21).

```bash
cd frontend && npm install && npm start   # http://localhost:4200, set FRONTEND_ORIGIN accordingly
```
