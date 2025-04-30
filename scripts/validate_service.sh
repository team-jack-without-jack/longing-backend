#!/bin/bash
set -e
set -x

# (1) 초기 대기: Spring Boot + Nginx가 뜰 시간을 줌
sleep 15

# (2) 체크 횟수·인터벌 정의
MAX_TRIES=12
INTERVAL=10

# (3) 포트 80으로 체크 (Nginx 리버스 프록시 경유)
URL="http://localhost/actuator/health"

for (( i=1; i<=MAX_TRIES; i++ )); do
  if curl --silent --fail "$URL"; then
    echo "✔ Service is healthy (attempt $i)"
    exit 0
  fi
  echo "❗ Service not ready yet, retrying ($i/$MAX_TRIES)…"
  sleep $INTERVAL
done

echo "🚨 Service failed to respond at $URL after $((MAX_TRIES*INTERVAL))s"
exit 1
