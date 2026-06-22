# syntax=docker/dockerfile:1
# 1) 빌드: JDK 21 로 bootJar
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true
COPY src src
RUN ./gradlew --no-daemon clean bootJar -x test

# 2) 런타임: JRE 21 슬림
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/mk-cli-compare-backend-0.0.1-SNAPSHOT.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
# SQLite 영구 볼륨 마운트 지점
VOLUME ["/data"]
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
