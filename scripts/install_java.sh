#!/bin/bash
set -e

# Amazon Linux 2: Corretto 11 via amazon-linux-extras
if ! yum list installed java-11-amazon-corretto-devel &>/dev/null; then
  # 1) extras 활성화
  amazon-linux-extras enable corretto11
  yum clean metadata

  # 2) Corretto 설치
  yum install -y java-11-amazon-corretto-devel
fi

# (선택) OpenJDK 11 설치 예시
# yum install -y java-11-openjdk-devel
