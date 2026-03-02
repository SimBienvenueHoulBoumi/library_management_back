## Étape de build
FROM maven:3.9.4-eclipse-temurin-17 AS builder

WORKDIR /app

# Mise à jour des certificats CA pour éviter "SSL peer shut down incorrectly" avec Maven Central
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates && \
    update-ca-certificates && rm -rf /var/lib/apt/lists/*

COPY . .

# Build du jar Spring Boot (TLS 1.2 pour Maven Central si SSL handshake échoue)
ENV MAVEN_OPTS="-Dhttps.protocols=TLSv1.2"
RUN mvn clean install -DskipTests -B

## Étape finale (runtime)
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Port HTTP de l'application (par défaut 8080)
EXPOSE 8075

ENTRYPOINT ["java", "-jar", "app.jar"]


