#!/usr/bin/env bash
# Manual security checks for Buy-02 (see docs/security.md).
# Uses the demo accounts from docs/demo-accounts.md: run ./scripts/seed.sh first.
#
#   ./scripts/security-check.sh
#
# Optional overrides: G (gateway URL, default https://localhost:8443), ORIGIN (allowed CORS
# origin), CLIENT_EMAIL / CLIENT_PASSWORD / SELLER_EMAIL / SELLER_PASSWORD.
# Side effect: clears the cart of the client account it uses (the seed creates no carts).

set -u

G="${G:-https://localhost:8443}"
ORIGIN="${ORIGIN:-http://localhost:4200}"
CLIENT_EMAIL="${CLIENT_EMAIL:-client.one@demo.example}"
CLIENT_PASSWORD="${CLIENT_PASSWORD:-DemoOnly2026!}"
SELLER_EMAIL="${SELLER_EMAIL:-seller.one@demo.example}"
SELLER_PASSWORD="${SELLER_PASSWORD:-DemoOnly2026!}"

PASS=0
FAIL=0

ok()   { echo "PASS  $1"; PASS=$((PASS + 1)); }
fail() { echo "FAIL  $1  (got: $2)"; FAIL=$((FAIL + 1)); }

# expect <status> <description> <curl args...>
expect() {
  local want="$1" desc="$2"; shift 2
  local got
  got=$(curl -sk -o /dev/null -w "%{http_code}" "$@")
  if [ "$got" = "$want" ]; then ok "$desc"; else fail "$desc" "$got"; fi
}

login() {
  curl -sk "$G/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$2\"}" | grep -o '"data":"[^"]*' | cut -d'"' -f4
}

# Fresh tokens on every run (they expire after one hour)
CLIENT=$(login "$CLIENT_EMAIL" "$CLIENT_PASSWORD")
SELLER=$(login "$SELLER_EMAIL" "$SELLER_PASSWORD")
if [ -z "$CLIENT" ] || [ -z "$SELLER" ]; then
  echo "Login failed: run ./scripts/seed.sh to create the demo accounts, or set the *_EMAIL / *_PASSWORD overrides." >&2
  exit 2
fi
AC=(-H "Authorization: Bearer $CLIENT")
AS=(-H "Authorization: Bearer $SELLER")
JSON=(-H 'Content-Type: application/json')

echo "== Public reads (no token)"
expect 200 "GET /products"                 "$G/products"
expect 200 "GET /products/search"          "$G/products/search?q=a"
expect 200 "GET /products/facets"          "$G/products/facets"

echo "== No token -> 401"
expect 401 "POST /products"                -X POST "$G/products"   "${JSON[@]}" -d '{}'
expect 401 "POST /cart/items"              -X POST "$G/cart/items" "${JSON[@]}" -d '{}'
expect 401 "POST /orders"                  -X POST "$G/orders"     "${JSON[@]}" -d '{}'
expect 401 "GET /cart"                     "$G/cart"
expect 401 "GET /cart with invalid token"  "$G/cart" -H "Authorization: Bearer not-a-real-token"

if curl -sk -i "$G/cart" | grep -qi '^set-cookie'; then
  fail "No session cookie on rejected request" "Set-Cookie present"
else
  ok "No session cookie on rejected request"
fi

echo "== Roles -> 403"
expect 403 "SELLER cannot use the cart"           "$G/cart" "${AS[@]}"
expect 403 "CLIENT cannot list seller orders"     "$G/orders/seller" "${AC[@]}"
expect 403 "CLIENT cannot create a product"       -X POST "$G/products" "${AC[@]}" "${JSON[@]}" \
  -d '{"name":"Check","description":"Security check","price":10,"quantity":5,"category":"ELECTRONICS"}'
expect 200 "CLIENT can read own cart"             "$G/cart" "${AC[@]}"
expect 200 "SELLER can list own orders"           "$G/orders/seller" "${AS[@]}"
expect 404 "Unknown path with valid token -> 404" "$G/does-not-exist" "${AC[@]}"

echo "== Spoofed identity headers are overwritten by the gateway"
P=$(curl -sk "$G/products" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
if [ -z "$P" ]; then
  fail "Spoofing check" "no product found to add to the cart"
else
  curl -sk -o /dev/null -X POST "$G/cart/items" "${AC[@]}" "${JSON[@]}" -d "{\"productId\":\"$P\",\"quantity\":1}"
  BODY=$(curl -sk "$G/cart" "${AC[@]}" -H "X-User-Id: someone-else" -H "X-User-Role: SELLER")
  if echo "$BODY" | grep -q "$P"; then ok "Fake X-User-Id / X-User-Role ignored"; else fail "Fake X-User-Id / X-User-Role ignored" "$BODY"; fi
  curl -sk -o /dev/null -X DELETE "$G/cart" "${AC[@]}"     # clean up
fi

echo "== CORS"
if curl -sk -i -X OPTIONS "$G/cart" -H "Origin: https://evil.example" -H "Access-Control-Request-Method: POST" \
   | grep -qi '^access-control-allow-origin'; then
  fail "Foreign origin is refused" "Access-Control-Allow-Origin present"
else
  ok "Foreign origin is refused"
fi
if curl -sk -i -X OPTIONS "$G/cart" -H "Origin: $ORIGIN" -H "Access-Control-Request-Method: POST" \
   | grep -qi "^access-control-allow-origin: $ORIGIN"; then
  ok "Allowed origin is accepted ($ORIGIN)"
else
  fail "Allowed origin is accepted ($ORIGIN)" "no matching Access-Control-Allow-Origin"
fi

echo "== Security headers"
HEADERS=$(curl -sk -i "$G/products")
for h in 'strict-transport-security' 'x-content-type-options: nosniff' 'x-frame-options: deny'; do
  if echo "$HEADERS" | grep -qi "^$h"; then ok "Header: $h"; else fail "Header: $h" "missing"; fi
done

echo
echo "Result: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
