#!/bin/bash

# AWS CLI를 사용하여 Secrets Manager에서 시크릿을 가져옵니다.
SECRET_NAME="longing/prod"   # 가져올 시크릿 이름
REGION="ap-northeast-2"      # 리전

# Secrets Manager에서 시크릿을 가져와서 변수에 저장
SECRET_VALUE=$(aws secretsmanager get-secret-value --secret-id $SECRET_NAME --region $REGION --query SecretString --output text)

# 시크릿 값 확인 (디버깅 용도)
echo "Fetched secret value: $SECRET_VALUE"

# 시크릿을 환경 변수로 설정
export MY_SECRET=$SECRET_VALUE

# 환경 변수를 시스템 환경 변수로 저장하거나, 애플리케이션에서 사용하도록 설정할 수 있습니다.
echo "MY_SECRET=$MY_SECRET" >> /etc/environment
source /etc/environment
