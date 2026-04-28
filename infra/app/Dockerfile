FROM gradle:8.11-jdk21-alpine AS builder

WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY src ./src

RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

USER spring:spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
