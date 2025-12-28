## Étape de build
FROM maven:3.9.4-eclipse-temurin-17 AS builder

WORKDIR /app
COPY . .

# Build du jar Spring Boot en sautant les tests (ils sont déjà exécutés par Jenkins)
RUN mvn clean install -DskipTests

## Étape finale (runtime)
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Port HTTP de l'application (par défaut 8080)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]


