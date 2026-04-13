# ─── Runtime-only ─────────────────────────────────────────────────
# O JAR é compilado pelo Jenkinsfile (./gradlew bootJar) antes do docker build.
# Este Dockerfile somente empacota o artefato pré-compilado na imagem runtime.
# O JAR é copiado para a imagem e o comando de execução é configurado para iniciar a aplicação.
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S -u 1001 -G appgroup appuser

WORKDIR /app

COPY build/libs/base-app.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=60.0", "-XX:InitialRAMPercentage=30.0", "-jar", "app.jar"]
