# 1) 빌드 스테이지
FROM gradle:8.8-jdk17-alpine AS build
WORKDIR /app

# gradle 캐시 최적화용: 먼저 빌드 스크립트만 복사
COPY build.gradle .
COPY settings.gradle .

# 소스 나머지는 나중에 복사 (의존성 캐시 분리)
RUN gradle dependencies --no-daemon || true

# 실제 전체 복사
COPY . .

# 애플리케이션 JAR 빌드
RUN gradle clean bootJar --no-daemon

# 2) 런타임 스테이지 (경량 JRE)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 빌드 결과물만 복사
COPY --from=build /app/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
