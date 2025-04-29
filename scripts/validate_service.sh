#!/bin/bash
set -e
set -x

URL="http://localhost:8080/actuator/health"

# 최대 12회, 10초 간격으로 재시도
for i in {1..12}; do
  if curl --silent --fail "$URL"; then
    echo "✔ Service is up"
    exit 0
  fi
  echo "❗ Service not ready yet, retrying ($i/12)..."
  sleep 10
done

echo "🚨 Service failed to respond at $URL"
exit 1

