##!/bin/bash
#set -e
#
#BUCKET=longing-op-config
#KEY=application-prod.yml
#DEST=/home/ec2-user/app/config
#
## (1) awscli 설치 확인 (없으면 설치)
#command -v aws >/dev/null || yum install -y awscli
#
## (2) 디렉터리 준비
#mkdir -p $DEST
#
## (3) S3에서 내려받기 (디버그 출력 추가)
#echo "Downloading from s3://$BUCKET/$KEY to $DEST/$KEY"
#aws s3 cp s3://$BUCKET/$KEY $DEST/$KEY --debug
#
## (4) 소유권/퍼미션
#chown ec2-user:ec2-user $DEST/$KEY
#chmod 600 $DEST/$KEY






#!/bin/bash
set -e

SECRET_NAME="prod/longing/config"      # Secrets Manager에 저장된 키
REGION="ap-northeast-2"         # 리전
TARGET="/home/ec2-user/app/config/application-prod.yml"   # 생성할 경로

# 시크릿 문자열(plain text YAML) 직접 가져와 파일로 저장
aws secretsmanager get-secret-value \
  --secret-id "$SECRET_NAME" --region "$REGION" \
  --query SecretString --output text \
  > "$TARGET"

chmod 640 "$TARGET"             # 권한 설정
