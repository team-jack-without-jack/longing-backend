#!/bin/bash
set -e
set -x

LOG_KEY=logback-spring.xml
APP_DIR=/home/ec2-user/app
CONFIG_DIR=$APP_DIR/config
JAR_NAME=longing-0.0.1-SNAPSHOT.jar

# (1) JAVA_HOME 설정
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
export PATH=$JAVA_HOME/bin:$PATH

# (2) 작업 디렉터리로 이동
cd /home/ec2-user/app

mkdir -p /home/ec2-user/logs

# (3) 백그라운드로 실행 & 로그
#nohup $JAVA_HOME/bin/java -jar longing-0.0.1-SNAPSHOT.jar \
#  --spring.profiles.active=prod \
#  > app.out 2>&1 &

#nohup $JAVA_HOME/bin/java \
#  -Dlogging.config=file:$CONFIG_DIR/logback-spring.xml \
#  -jar $APP_DIR/$JAR_NAME \
#  --spring.profiles.active=prod \
#  --spring.config.additional-location=classpath:/,file:$CONFIG_DIR/ \
#  > $APP_DIR/app.out 2>&1 &

nohup $JAVA_HOME/bin/java \
  -Dlogging.config=file:$CONFIG_DIR/logback-spring.xml \
  -jar $APP_DIR/$JAR_NAME \
  --spring.profiles.active=prod \
  --spring.config.additional-location=file:$CONFIG_DIR/ \
  > $APP_DIR/app.out 2>&1 &

# (4) 바로 실행 확인
sleep 5
ps aux | grep "[l]onging-0.0.1-SNAPSHOT.jar"