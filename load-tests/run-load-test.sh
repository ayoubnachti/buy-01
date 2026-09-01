#!/usr/bin/env bash
set -euo pipefail

# Runs the k6 load test against the local stack using the official k6 Docker image.
# Uses --network host so the container can reach services on localhost (Linux-friendly).

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_FILE="${1:-$SCRIPT_DIR/seller-load-test.js}"

if [ ! -f "$TEST_FILE" ]; then
  echo "Test script not found: $TEST_FILE"
  echo "Usage: ./run-load-test.sh [path-to-test.js]"
  exit 1
fi

echo "Running k6 load test: $TEST_FILE"
echo "----------------------------------------"

docker run --rm -i \
  --network host \
  grafana/k6 run - < "$TEST_FILE"
