FROM docker.io/library/gradle:8.11-jdk21-alpine AS builder

WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY src ./src

RUN ./gradlew bootJar -x test --no-daemon

FROM docker.io/library/eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S -g 1000 spring && adduser -S -u 1000 -G spring spring \
    && mkdir -p /app/logs \
    && chown -R spring:spring /app/logs

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

USER spring:spring

EXPOSE 8080 9090

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=5 \
    CMD wget -qO- http://127.0.0.1:${MANAGEMENT_PORT:-9090}/actuator/health >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
