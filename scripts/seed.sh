#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required to verify the running application profile." >&2
  exit 1
fi

profile=""
for service in api-gateway user-service product-service media-service order-service; do
  service_profile="$(docker compose exec -T "$service" printenv SPRING_PROFILES_ACTIVE 2>/dev/null || true)"
  case "$service_profile" in
    dev|staging) ;;
    *)
      echo "Refusing to seed: $service must be running with SPRING_PROFILES_ACTIVE=dev or staging." >&2
      exit 1
      ;;
  esac

  if [[ -z "$profile" ]]; then
    profile="$service_profile"
  elif [[ "$profile" != "$service_profile" ]]; then
    echo "Refusing to seed: application services use different profiles." >&2
    exit 1
  fi
done

export DEMO_ACTIVE_PROFILE="$profile"
exec python3 scripts/seed_demo_data.py
