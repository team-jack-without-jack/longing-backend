#!/bin/bash
set -e

# 1) 프록시 설정 파일 작성
cat > /etc/nginx/conf.d/app.conf <<EOF
server {
    listen       80;
    server_name  _;

    location / {
        proxy_pass         http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header   Host \$host;
        proxy_set_header   X-Real-IP \$remote_addr;
        proxy_set_header   X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto \$scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout    60s;
    }
}
EOF

# 2) 설정 문법 검증 & 재시작
nginx -t
systemctl reload nginx
