# Demo accounts

These are reserved demo accounts for local development and staging only. Do not
use them for real people, production data, or sensitive information.

| Role | Name | Email | Demo password |
|---|---|---|---|
| Seller | Demo Seller One | `seller.one@demo.example` | `DemoOnly2026!` |
| Seller | Demo Seller Two | `seller.two@demo.example` | `DemoOnly2026!` |
| Client | Demo Client One | `client.one@demo.example` | `DemoOnly2026!` |
| Client | Demo Client Two | `client.two@demo.example` | `DemoOnly2026!` |
| Client | Demo Client Three | `client.three@demo.example` | `DemoOnly2026!` |

`./scripts/seed.sh` creates these accounts if missing, then resets their demo
orders and the products named with the `Demo ` prefix before reseeding. The
three client accounts are reserved: reset permanently deletes all orders owned
by them from the dev/staging order database.
The script creates 30 products, 40 orders with all order statuses and dates
spread across the last 60 days, and associates product cards with the local
placeholder SVG images in `frontend/public/assets/demo/`.

Orders are created and transitioned through the real order API so inventory,
ownership, snapshots, totals, and allowed status transitions follow application
rules. The API does not accept historical timestamps, so after creation the
script backdates only each seeded order's `createdAt`, `updatedAt`, and status
history in the local MongoDB container for dashboard date ranges.

## Running the seed

1. Copy `.env.example` to `.env` and set the local development secrets.
2. Keep `APP_PROFILE=dev` in `.env`, then start the stack with
   `docker compose up -d --wait`.
3. Run `./scripts/seed.sh`.

The script verifies the active profile in the gateway, user, product, and order
containers, including media-service. It refuses to run unless all five are on
`dev` or all five are on `staging`; Compose defaults the profile to `prod` when
it is unset. Run the command on the same host and Compose project as the target
database. For staging, set `APP_PROFILE=staging` in that stack's Compose
environment. Set
`DEMO_API_URL` if the gateway is not at `https://localhost:8443`; it must still
use a localhost address on the target Compose host so the API and MongoDB writes
reach the same environment. Set
`DEMO_IMAGE_BASE_URL` if the frontend serves the placeholder files at another
URL. A local self-signed certificate is accepted only when the verified active
profile is `dev`; for staging, use a trusted certificate or provide
`DEMO_CA_BUNDLE`.

The seed requires Docker Compose, Python 3, and a running stack. It does not
upload files to Cloudinary; the placeholder SVGs are served by the frontend and
referenced through the media database.
