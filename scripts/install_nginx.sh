#!/bin/bash
set -e
set -x

# 1) amazon-linux-extras에 nginx1 토픽 활성화
if ! yum list installed nginx &>/dev/null; then
  amazon-linux-extras enable nginx1
  yum clean metadata
  yum install -y nginx
fi

# 2) 서비스 등록 및 시작
systemctl enable nginx
systemctl start nginx

# 3) (선택) 상태 확인
systemctl status nginx
