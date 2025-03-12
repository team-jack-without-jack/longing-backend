#!/bin/bash

# 설정
APP_NAME=longing-0.0.1-SNAPSHOT.jar
S3_BUCKET=deploy-build-file
DEPLOY_DIR=/home/ec2-user/action
LOG_FILE=$DEPLOY_DIR/deploy.log
APP_PORT=8080

echo "===== 배포 시작: $(date) =====" | tee -a $LOG_FILE

# AWS S3에서 최신 JAR 다운로드
echo "> S3에서 최신 JAR 다운로드" | tee -a $LOG_FILE
aws s3 cp s3://$S3_BUCKET/$APP_NAME $DEPLOY_DIR/ | tee -a $LOG_FILE

# 실행 중인 애플리케이션 확인 및 종료
CURRENT_PID=$(lsof -ti :$APP_PORT)
if [ -n "$CURRENT_PID" ]; then
  echo "> 실행 중인 애플리케이션 종료 (PID: $CURRENT_PID)" | tee -a $LOG_FILE
  kill -9 $CURRENT_PID
  sleep 5
else
  echo "> 실행 중인 애플리케이션 없음" | tee -a $LOG_FILE
fi

# application.yml 생성 (GitHub Secrets에서 받은 값 사용)
echo "> 환경 설정 파일 생성" | tee -a $LOG_FILE
echo "${APPLICATION_YML}" | base64 --decode > /home/ec2-user/config/application.yml

# 새로운 애플리케이션 실행
echo "> 새 애플리케이션 실행" | tee -a $LOG_FILE
nohup java -jar -Dspring.config.location=/home/ec2-user/config/application.yml $DEPLOY_DIR/$APP_NAME > $DEPLOY_DIR/app.log 2>&1 &

echo "> 배포 완료!" | tee -a $LOG_FILE
