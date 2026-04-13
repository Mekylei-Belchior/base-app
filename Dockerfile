# ─── Stage 1: Build ────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /build

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# Download dependencies first (layer cache friendly)
RUN ./gradlew dependencies --no-daemon -q || true

COPY src ./src

RUN ./gradlew bootJar --no-daemon

# ─── Stage 2: Runtime ───────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S -u 1001 -G appgroup appuser

WORKDIR /app

COPY --from=build /build/build/libs/base-app.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=60.0", "-XX:InitialRAMPercentage=30.0", "-jar", "app.jar"]
