#!/usr/bin/env python3
"""Reset and seed reserved demo accounts using the application APIs."""

from __future__ import annotations

import base64
import json
import os
import ssl
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timedelta, timezone


DEMO_PASSWORD = "DemoOnly2026!"
PRODUCT_NAME_PREFIX = "Demo "
ORDER_STATUSES = ("PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED")
IMAGE_FILES = ("keyboard.svg", "lamp.svg", "chair.svg")

SELLER_ACCOUNTS = (
    {"name": "Demo Seller One", "email": "seller.one@demo.example", "role": "SELLER"},
    {"name": "Demo Seller Two", "email": "seller.two@demo.example", "role": "SELLER"},
)
CLIENT_ACCOUNTS = (
    {"name": "Demo Client One", "email": "client.one@demo.example", "role": "CLIENT"},
    {"name": "Demo Client Two", "email": "client.two@demo.example", "role": "CLIENT"},
    {"name": "Demo Client Three", "email": "client.three@demo.example", "role": "CLIENT"},
)
ACCOUNT_BOOK = SELLER_ACCOUNTS + CLIENT_ACCOUNTS

CATEGORIES = (
    ("ELECTRONICS", "Wireless keyboard"),
    ("CLOTHING", "Everyday jacket"),
    ("BOOKS", "Design handbook"),
    ("HOME", "Desk lamp"),
    ("SPORTS", "Training bottle"),
)
PRICES = ("19.99", "24.50", "32.00", "45.75", "59.00", "84.25")
STOCK_LEVELS = (8, 12, 25, 40, 60, 90)


class SeedError(RuntimeError):
    pass


class ApiError(SeedError):
    def __init__(self, status: int, message: str):
        super().__init__(message)
        self.status = status


class ApiClient:
    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip("/")
        parsed = urllib.parse.urlparse(self.base_url)
        if parsed.scheme not in ("http", "https") or not parsed.netloc:
            raise SeedError("DEMO_API_URL must be an absolute http(s) URL.")
        if parsed.hostname not in ("localhost", "127.0.0.1", "::1"):
            raise SeedError(
                "Run the seed on the target Compose host; DEMO_API_URL must use localhost."
            )
        if parsed.scheme == "http" and os.environ.get("DEMO_ACTIVE_PROFILE") != "dev":
            raise SeedError("Plain HTTP is allowed only for a local dev profile.")

        self.ssl_context = None
        if parsed.scheme == "https":
            ca_bundle = os.environ.get("DEMO_CA_BUNDLE")
            if ca_bundle:
                self.ssl_context = ssl.create_default_context(cafile=ca_bundle)
            elif os.environ.get("DEMO_ACTIVE_PROFILE") == "dev" and parsed.hostname in (
                "localhost",
                "127.0.0.1",
                "::1",
            ):
                self.ssl_context = ssl._create_unverified_context()
            else:
                self.ssl_context = ssl.create_default_context()

    def request(self, method: str, path: str, token: str | None = None, body=None):
        data = None if body is None else json.dumps(body).encode("utf-8")
        headers = {"Accept": "application/json"}
        if data is not None:
            headers["Content-Type"] = "application/json"
        if token:
            headers["Authorization"] = f"Bearer {token}"

        request = urllib.request.Request(
            self.base_url + path, data=data, headers=headers, method=method
        )
        try:
            with urllib.request.urlopen(
                request, context=self.ssl_context, timeout=30
            ) as response:
                payload = response.read()
                if not payload:
                    return {}
                result = json.loads(payload)
        except urllib.error.HTTPError as error:
            details = error.read().decode("utf-8", errors="replace")
            raise ApiError(
                error.code, f"{method} {path} failed with HTTP {error.code}: {details}"
            ) from error
        except urllib.error.URLError as error:
            raise SeedError(f"Cannot reach {self.base_url}: {error.reason}") from error

        if isinstance(result, dict) and result.get("success") is False:
            raise SeedError(f"{method} {path} failed: {result.get('message', 'unknown error')}")
        return result


def product_specs():
    specs = []
    for category, label in CATEGORIES:
        for offset in range(6):
            number = offset + 1
            specs.append(
                {
                    "name": f"{PRODUCT_NAME_PREFIX}{category.title()} {number:02d}",
                    "description": (
                        f"{label} demo product {number}. Curated sample inventory "
                        "for the dashboards, audit demo, and end-to-end tests."
                    ),
                    "price": PRICES[offset],
                    "quantity": STOCK_LEVELS[offset],
                    "category": category,
                    "seller_index": len(specs) % len(SELLER_ACCOUNTS),
                }
            )
    return specs


def order_plan(now: datetime | None = None):
    now = now or datetime.now(timezone.utc)
    if now.tzinfo is None:
        raise ValueError("now must be timezone-aware")
    now = now.astimezone(timezone.utc)
    plan = []
    for index in range(40):
        age_days = ((index * 17) % 59) + 1
        created_at = now - timedelta(days=age_days, hours=index % 23)
        plan.append(
            {
                "client_index": index % len(CLIENT_ACCOUNTS),
                "product_index": (index * 7) % 30,
                "quantity": 1 + index % 3,
                "status": ORDER_STATUSES[index % len(ORDER_STATUSES)],
                "created_at": created_at,
            }
        )
    return plan


def account_token(api: ApiClient, account: dict):
    login_path = "/auth/login"
    credentials = {"email": account["email"], "password": DEMO_PASSWORD}
    try:
        response = api.request("POST", login_path, body=credentials)
    except ApiError as error:
        if error.status != 401:
            raise
        registration = {
            "name": account["name"],
            **credentials,
            "role": account["role"],
        }
        try:
            api.request("POST", "/auth/register", body=registration)
        except ApiError as register_error:
            # A registration conflict can occur if a previous interrupted run created
            # the account. Login below confirms whether the expected demo password works.
            if register_error.status not in (400, 409):
                raise
        response = api.request("POST", login_path, body=credentials)

    token = response.get("data")
    if not isinstance(token, str):
        raise SeedError(f"Login for {account['email']} returned no token.")
    claims = jwt_claims(token)
    if claims.get("role") != account["role"] or not claims.get("userId"):
        raise SeedError(f"Account {account['email']} has an unexpected role or ID.")
    return {
        **account,
        "id": claims["userId"],
        "token": token,
    }


def jwt_claims(token: str):
    parts = token.split(".")
    if len(parts) != 3:
        raise SeedError("Login returned an invalid JWT.")
    payload = parts[1] + "=" * (-len(parts[1]) % 4)
    try:
        claims = json.loads(base64.urlsafe_b64decode(payload))
    except (ValueError, json.JSONDecodeError) as error:
        raise SeedError("Login returned an unreadable JWT.") from error
    return claims


def response_data(response):
    if not isinstance(response, dict) or not isinstance(response.get("data"), dict):
        raise SeedError("The API returned an unexpected response shape.")
    return response["data"]


def response_list(response):
    if not isinstance(response, dict) or not isinstance(response.get("data"), list):
        raise SeedError("The API returned an unexpected response shape.")
    return response["data"]


def reset_demo_orders(client_ids: list[str]):
    payload = json.dumps(client_ids, separators=(",", ":"))
    run_mongo_script(
        f"""
const clientIds = {payload};
db.getSiblingDB("orders_db").getCollection("orders")
  .deleteMany({{userId: {{$in: clientIds}}}});
"""
    )


def list_demo_products(api: ApiClient, seller: dict):
    demo_products = []
    first = response_data(
        api.request(
            "GET",
            f"/products?owner={urllib.parse.quote(seller['id'])}&page=0&limit=100",
            seller["token"],
        )
    )
    product_pages = [first]
    for page in range(1, first.get("totalPages", 1)):
        product_pages.append(
            response_data(
                api.request(
                    "GET",
                    f"/products?owner={urllib.parse.quote(seller['id'])}&page={page}&limit=100",
                    seller["token"],
                )
            )
        )
    for product_page in product_pages:
        demo_products.extend(
            product
            for product in product_page.get("items", [])
            if product.get("name", "").startswith(PRODUCT_NAME_PREFIX)
        )
    return demo_products


def run_mongo_script(script: str):
    command = [
        "docker",
        "compose",
        "exec",
        "-T",
        "mongodb",
        "mongosh",
        "--quiet",
        "mongodb://localhost:27017",
    ]
    try:
        result = subprocess.run(
            command, input=script, text=True, capture_output=True, check=False
        )
    except OSError as error:
        raise SeedError(f"Could not run MongoDB shell: {error}") from error
    if result.returncode:
        raise SeedError(
            "MongoDB seed operation failed: "
            + (result.stderr.strip() or result.stdout.strip())
        )


def replace_placeholder_media(old_product_ids: list[str], products: list[dict]):
    image_base = os.environ.get(
        "DEMO_IMAGE_BASE_URL", "http://localhost:4200/assets/demo"
    ).rstrip("/")
    media = [
        {
            "product_id": product["id"],
            "image_path": f"{image_base}/{IMAGE_FILES[index % len(IMAGE_FILES)]}",
        }
        for index, product in enumerate(products)
    ]
    payload = json.dumps(
        {"oldProductIds": old_product_ids, "media": media},
        separators=(",", ":"),
    )
    run_mongo_script(
        f"""
const seed = {payload};
const mediaCollection = db.getSiblingDB("media_db").getCollection("media");
if (seed.oldProductIds.length) {{
  mediaCollection.deleteMany({{product_id: {{$in: seed.oldProductIds}}}});
}}
if (seed.media.length) {{
  mediaCollection.insertMany(seed.media.map(item => ({{
    image_path: item.image_path,
    product_id: item.product_id
  }})));
}}
"""
    )


def status_history(status: str, created_at: datetime, user_id: str, seller_id: str):
    transitions = {
        "PENDING": [],
        "CONFIRMED": [("CONFIRMED", "seller", 1)],
        "SHIPPED": [("CONFIRMED", "seller", 1), ("SHIPPED", "seller", 8)],
        "DELIVERED": [
            ("CONFIRMED", "seller", 1),
            ("SHIPPED", "seller", 8),
            ("DELIVERED", "seller", 24),
        ],
        "CANCELLED": [("CANCELLED", "client", 1)],
    }
    history = [
        {
            "status": "PENDING",
            "at": created_at.isoformat(),
            "by": user_id,
        }
    ]
    for next_status, actor, hours in transitions[status]:
        history.append(
            {
                "status": next_status,
                "at": (created_at + timedelta(hours=hours)).isoformat(),
                "by": user_id if actor == "client" else seller_id,
            }
        )
    return history


def backdate_orders(rows: list[dict]):
    if not rows:
        return
    payload = json.dumps(rows, separators=(",", ":"))
    run_mongo_script(
        f"""
const rows = {payload};
const orders = db.getSiblingDB("orders_db").getCollection("orders");
for (const row of rows) {{
  const updated = orders.updateOne(
    {{_id: ObjectId(row.id)}},
    {{$set: {{
      createdAt: new Date(row.createdAt),
      updatedAt: new Date(row.updatedAt),
      statusHistory: row.statusHistory.map(entry => ({{
        status: entry.status,
        at: new Date(entry.at),
        by: entry.by
      }}))
    }}}}
  );
  if (updated.matchedCount !== 1) {{
    throw new Error(`Seed order ${{row.id}} was not found`);
  }}
}}
"""
    )


def create_products(api: ApiClient, sellers: list[dict]):
    products = []
    for spec in product_specs():
        seller = sellers[spec["seller_index"]]
        request = {
            key: value
            for key, value in spec.items()
            if key not in ("seller_index",)
        }
        request["imageUrls"] = []
        product = response_data(
            api.request("POST", "/products", seller["token"], request)
        )
        if not product.get("id"):
            raise SeedError(f"Product creation returned no ID for {spec['name']}.")
        products.append({**product, "seller_id": seller["id"]})
    return products


def create_orders(
    api: ApiClient, clients: list[dict], sellers: list[dict], products: list[dict]
):
    plan = order_plan()
    date_updates = []
    sellers_by_id = {seller["id"]: seller for seller in sellers}
    for index, order in enumerate(plan):
        client = clients[order["client_index"]]
        product = products[order["product_index"]]
        body = {
            "shippingAddress": {
                "fullName": client["name"],
                "phone": "+1-555-0100",
                "street": f"{100 + index} Demo Market Street",
                "city": "Sample City",
                "postalCode": f"{10000 + index:05d}",
                "country": "Demo Country",
            },
            "items": [
                {"productId": product["id"], "quantity": order["quantity"]}
            ],
        }
        created = response_list(
            api.request("POST", "/orders", client["token"], body)
        )
        if len(created) != 1 or not isinstance(created[0], dict):
            raise SeedError("Expected the order API to create exactly one order.")
        order_id = created[0].get("id")
        if not order_id:
            raise SeedError("Order creation returned no order ID.")

        seller = sellers_by_id[product["seller_id"]]
        final_status = order["status"]
        if final_status == "CANCELLED":
            api.request("POST", f"/orders/{order_id}/cancel", client["token"])
        else:
            for status in {
                "PENDING": [],
                "CONFIRMED": ["CONFIRMED"],
                "SHIPPED": ["CONFIRMED", "SHIPPED"],
                "DELIVERED": ["CONFIRMED", "SHIPPED", "DELIVERED"],
            }[final_status]:
                api.request(
                    "PATCH",
                    f"/orders/{order_id}/status",
                    seller["token"],
                    {"status": status},
                )

        history = status_history(
            final_status, order["created_at"], client["id"], seller["id"]
        )
        date_updates.append(
            {
                "id": order_id,
                "createdAt": order["created_at"].isoformat(),
                "updatedAt": history[-1]["at"],
                "statusHistory": history,
            }
        )
    backdate_orders(date_updates)


def main():
    profile = os.environ.get("DEMO_ACTIVE_PROFILE")
    if profile not in ("dev", "staging"):
        raise SeedError("Refusing to seed outside a dev or staging profile.")

    base_url = os.environ.get("DEMO_API_URL", "https://localhost:8443")
    api = ApiClient(base_url)
    accounts = {
        account["email"]: account_token(api, account) for account in ACCOUNT_BOOK
    }
    sellers = [accounts[account["email"]] for account in SELLER_ACCOUNTS]
    clients = [accounts[account["email"]] for account in CLIENT_ACCOUNTS]

    reset_demo_orders([client["id"] for client in clients])

    old_products = []
    for seller in sellers:
        old_products.extend(list_demo_products(api, seller))
    old_product_ids = [product["id"] for product in old_products]
    replace_placeholder_media(old_product_ids, [])
    for product in old_products:
        seller = next(s for s in sellers if s["id"] == product["userId"])
        api.request("DELETE", f"/products/{product['id']}", seller["token"])

    products = create_products(api, sellers)
    replace_placeholder_media([], products)
    create_orders(api, clients, sellers, products)

    print(
        "Demo data seeded successfully: "
        f"{len(SELLER_ACCOUNTS)} sellers, {len(CLIENT_ACCOUNTS)} clients, "
        f"{len(products)} products, {len(order_plan())} orders ({profile})."
    )


if __name__ == "__main__":
    try:
        main()
    except SeedError as error:
        print(f"Seed failed: {error}", file=sys.stderr)
        sys.exit(1)
