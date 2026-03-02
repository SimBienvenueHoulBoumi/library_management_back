# Image runtime uniquement : le JAR est déjà construit par le pipeline (./mvnw verify).
# Évite d'exécuter Maven dans le build Docker (plus d'erreur SSL avec Maven Central).
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY target/*.jar app.jar

# Port HTTP de l'application (par défaut 8080)
EXPOSE 8075

ENTRYPOINT ["java", "-jar", "app.jar"]
