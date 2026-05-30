
# ==========================================
# 1단계: Build Stage (Gradle로 빌드 및 계층 추출)
# ==========================================
FROM gradle:8.14.3-jdk21-alpine AS build
WORKDIR /app

# 의존성 설치를 위해 설정 파일만 먼저 복사 (캐싱 효율을 위해)
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon

# 소스 코드 복사 및 빌드 (테스트 제외)
COPY src ./src
RUN gradle bootJar --no-daemon -x test

# Layered JAR 추출 (이게 핵심! 변경된 부분만 나중에 빌드됨)
# 빌드된 jar 파일명을 찾아서 extract 수행
RUN java -Djarmode=layertools -jar build/libs/*.jar extract --destination extracted

# ==========================================
# 2단계: Run Stage (실제 실행 환경)
# ==========================================
FROM eclipse-temurin:21-jre-alpine-3.20
WORKDIR /app

# 보안을 위해 별도 유저 생성 (root 권한 사용 안 함)
RUN addgroup -S javauser && adduser -S javauser -G javauser
USER javauser

# 1단계에서 추출한 파일들을 순서대로 복사
# (의존성은 자주 안 바뀌니까 먼저 복사 -> 캐싱됨)
COPY --from=build /app/extracted/dependencies/ ./
COPY --from=build /app/extracted/spring-boot-loader/ ./
COPY --from=build /app/extracted/snapshot-dependencies/ ./
COPY --from=build /app/extracted/application/ ./

# 포트 노출
EXPOSE 8080

# 실행 명령어 (JarLauncher 사용)
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
# 만약 Spring Boot 3.2 미만 버전이라면 아래 주석을 사용하세요
# ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]