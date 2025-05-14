#!/bin/bash
./gradlew build -x test
java -Dspring.profiles.active=dev -jar build/libs/longing-0.0.1-SNAPSHOT.jar