# /scripts/install_java.sh 에 추가
#!/bin/bash
set -e

# 1) Corretto 11 RPM 다운로드
wget https://corretto.aws/downloads/latest/amazon-corretto-11-x64-linux-jdk.rpm

# 2) 설치
yum install -y amazon-corretto-11-x64-linux-jdk.rpm

# 3) 설치 확인
java -version