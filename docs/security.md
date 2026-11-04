# Security — Buy-02

How authentication, authorization and exposure work across the platform, and how to verify
each rule. Every rule below was checked against the running stack; `docs/security-check.sh`
repeats those checks.

## 1. How a request is authenticated

```
Browser ──HTTPS──► Caddy (staging) ──► api-gateway ──Docker network──► service
                                        │
                                        ├─ validates the JWT (signature, expiry)
                                        ├─ removes any X-User-Id / X-User-Role sent by the client
                                        └─ sets X-User-Id / X-User-Role from the token
```

- **user-service** issues the JWT at login (`POST /auth/login`). Lifetime: 1 hour.
- **api-gateway** decides only *"logged in or not"*. It is stateless: no HTTP session, no
  `JSESSIONID`. Missing, invalid or expired token on a protected route → **401**.
- **Each service** trusts the `X-User-*` headers (they can only come from the gateway, see §6)
  and checks **roles and ownership** itself. Wrong role → **403**.
- **Passwords** are stored hashed with BCrypt (user-service) and never returned.

## 2. Public endpoints (no token)

| Method | Path | Notes |
|---|---|---|
| POST | `/auth/register`, `/auth/login` | |
| GET | `/products` | List |
| GET | `/products/{id}` | Detail |
| GET | `/products/search` | Keyword + filters (product-service) |
| GET | `/products/facets` | Categories and price range |
| GET | `/actuator/health` | Gateway health |

Everything else requires a valid token.

**Media:** product images are served directly by Cloudinary (the public URLs returned in
`imageUrls`). The media-service endpoints are seller-only uploads and deletes, so none of
them are public.

## 3. Who can do what

| Area | Endpoint | CLIENT | SELLER | Enforced in |
|---|---|---|---|---|
| Products | `POST/PUT/DELETE /products...` | 403 | own products only | product-service |
| Media | upload / delete | 403 | own media only | media-service |
| Cart | all `/cart...` | ✅ own cart only | 403 | cart-service |
| Orders | `POST /orders`, `GET /orders/my` | ✅ | 403 | order-service |
| Orders | `GET /orders/seller`, `PATCH /orders/{id}/status` | 403 | ✅ own orders only | order-service |
| Orders | `GET /orders/{id}`, cancel | owner | seller of the order | order-service |
| Orders | redo, delete | owner | 403 | order-service |
| Profile | `/profile/user/stats` | ✅ | 403 | order-service (analytics) |
| Profile | `/profile/seller/stats` | 403 | ✅ | order-service (analytics) |

### Ownership rules

- The user is **always** taken from `X-User-Id`, never from a path or body. The cart has no
  `/cart/{userId}` endpoint, so there is nothing to guess.
- Another user's order or cart item answers **404**, not 403, so the response does not
  reveal that the resource exists.
- Prices, sellers and stock are always read from product-service at the time of the request;
  the client never sends a price, and the cart never stores one.

### 401 vs 403

| Situation | Status | Frontend meaning |
|---|---|---|
| No token, invalid token, expired token | **401** | Logged out → go to login |
| Valid token, wrong role or not the owner | **403** (404 for others' resources) | Logged in, not allowed |

Both the gateway and cart-service permit the servlet `ERROR` dispatch and never answer 403
through `sendError`, so Tomcat's internal `/error` forward cannot turn a 403 or 404 into 401.

## 4. CORS (gateway only)

- Allowed origin: **one**, from `CORS_ALLOWED_ORIGIN` (`http://localhost:4200` locally).
  Never `*`.
- Allowed methods: `GET, POST, PUT, PATCH, DELETE, OPTIONS`.
- Allowed headers: an explicit list (see `CorsConfig`).
- A preflight from any other origin gets no `Access-Control-Allow-Origin` header, so the
  browser blocks the call.

## 5. Transport and headers

- **Locally:** the gateway serves HTTPS only on `:8443` (self-signed keystore, generated and
  git-ignored). There is no plain-HTTP port to redirect.
- **Staging:** Caddy terminates TLS for `shop.ayoubnachti.dev` with an automatic certificate
  and redirects HTTP to HTTPS (**308**).
- Response headers set by Spring Security at the gateway:
  `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`,
  `Cache-Control: no-store`.

## 6. What is reachable from outside

| Port | Published as | Reachable from |
|---|---|---|
| 8443 (gateway) | `127.0.0.1:8443` | this machine only |
| 4200 (frontend) | `127.0.0.1:4200` | this machine only |
| 8761 (Eureka) | `127.0.0.1:8761` | this machine only |
| services, MongoDB | not published | Docker network only |

- On staging, Caddy reaches containers by name over the Docker network, so only Caddy
  (80/443) faces the internet. The OCI security list also blocks every other port
  (verified: 4200, 8443, 8761, 8080, 9000 give no answer from outside).
- Eureka accepts registrations without authentication. Exposing it would let anyone register
  a fake instance and receive part of the gateway's traffic; that is why it is never
  published beyond localhost.

## 7. Secrets

- `JWT_SECRET`, `CLOUDINARY_URL`, keystore password and Mongo URIs come from `.env`
  (git-ignored; `.env.example` has placeholders) locally, and from the Jenkins credential
  store in the pipeline.
- The `JWT_SECRET` and `CLOUDINARY_URL` values in the `Jenkinsfile` are **test-only** values
  for the test stages; production uses different secrets.
- Error responses never contain stack traces or internal details (catch-all handlers return
  a generic message; the cause is only logged).

## 8. Known limitations

- The demo accounts (`docs/demo-accounts.md`) have published passwords. They exist on dev and
  staging only; staging is publicly reachable, so anyone can log in as a demo user there.
  Acceptable for a demo environment, never for real users or data.
- Tokens last one hour with no refresh; the user logs in again.
- Calls between services inside the Docker network are not authenticated; the network is
  not reachable from outside (§6).
- Rate limiting is not implemented (optional in the brief).

## 9. How to verify

```bash
docker compose up -d --wait
./scripts/seed.sh              # demo accounts and products (docs/demo-accounts.md)
./scripts/security-check.sh
```

The check logs in as `client.one` and `seller.one` with fresh tokens, prints `PASS`/`FAIL`
per rule and exits non-zero if anything fails. It clears `client.one`'s cart at the end.
Other accounts can be used with `CLIENT_EMAIL`, `CLIENT_PASSWORD`, `SELLER_EMAIL`,
`SELLER_PASSWORD`.