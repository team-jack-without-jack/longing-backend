#!/bin/bash
nohup java -jar /home/ec2-user/app/longing-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod > /home/ec2-user/app/app.out 2>&1 &
