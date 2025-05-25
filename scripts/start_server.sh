#!/bin/bash
set -e
set -x

LOG_KEY=logback-spring.xml

# (1) JAVA_HOME 설정
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
export PATH=$JAVA_HOME/bin:$PATH

# (2) 작업 디렉터리로 이동
cd /home/ec2-user/app

# (3) 백그라운드로 실행 & 로그
#nohup $JAVA_HOME/bin/java -jar longing-0.0.1-SNAPSHOT.jar \
#  --spring.profiles.active=prod \
#  > app.out 2>&1 &

nohup $JAVA_HOME/bin/java \
  -Dlogging.config=/home/ec2-user/app/config/logback-spring.xml \
  -jar /home/ec2-user/app/longing-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --spring.config.additional-location=classpath:/,file:/home/ec2-user/app/config/ \
  > /home/ec2-user/app/app.out 2>&1 &

# (4) 바로 실행 확인
sleep 5
ps aux | grep "[l]onging-0.0.1-SNAPSHOT.jar"