# syntax=docker/dockerfile:1

# ============================================================
# Build stage
# 로컬(gradle/wrapper/gradle-wrapper.properties = 9.6.1)과 같은 Gradle로 빌드하기 위해
# 공식 gradle 이미지 대신 JDK 이미지 + Gradle Wrapper를 사용한다.
# gradle:8.10 이미지를 쓰면 로컬(9.6.1)과 CI의 빌드 도구 버전이 어긋난다.
# ============================================================
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# wrapper를 먼저 복사해서 Gradle 배포본(9.6.1) 다운로드를 별도 레이어로 굳힌다.
COPY gradlew ./
COPY gradle gradle

# Windows에서 커밋된 gradlew는 CRLF + 실행권한 없음 상태가 되기 쉽고,
# 그대로 두면 리눅스에서 "/bin/sh^M: bad interpreter" 또는 "permission denied"로 실패한다.
# .gitattributes로 예방하지만, 재체크아웃 환경에서도 깨지지 않도록 방어적으로 정규화한다.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# 빌드 스크립트만 먼저 복사한다. 소스만 바뀌면 아래 의존성 레이어는 재사용된다.
COPY settings.gradle build.gradle ./
RUN ./gradlew --no-daemon --console=plain dependencies --configuration runtimeClasspath

COPY src src
# 'build'가 아니라 'bootJar'만 실행한다.
# build를 쓰면 -plain.jar가 함께 생성되어, 실행 스테이지의 와일드카드 COPY가
# 두 파일에 매칭되면서 "destination must be a directory" 에러로 깨진다.
RUN ./gradlew --no-daemon --console=plain clean bootJar -x test

# ============================================================
# Run stage
# ============================================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# root로 실행하지 않는다.
RUN groupadd --system --gid 1001 app \
 && useradd --system --uid 1001 --gid app --no-create-home app

COPY --from=build --chown=app:app /workspace/build/libs/*.jar app.jar

USER app

# nginx가 이 포트로 프록시한다 (docker/nginx/nginx.conf의 upstream과 일치해야 함).
EXPOSE 8080

# EC2 t3.small(2GB)에 be/sim/nginx가 함께 뜬다. 힙 상한을 컨테이너 메모리의 50%로 고정한다.
# OOM 시에는 좀비 상태로 남기지 말고 종료시켜 restart 정책이 동작하게 한다.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=50.0 -XX:+ExitOnOutOfMemoryError"

# exec로 감싸 java가 PID 1이 되게 한다.
# 그래야 docker stop의 SIGTERM이 JVM에 전달되어 graceful shutdown이 이뤄진다.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
