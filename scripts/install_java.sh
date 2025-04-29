#!/bin/bash
set -e

# OpenJDK 11 설치 (실패 시 Java 8 설치)
yum install -y java-11-openjdk-devel || yum install -y java-1.8.0-openjdk-devel

# 설치 확인 (선택)
java -version