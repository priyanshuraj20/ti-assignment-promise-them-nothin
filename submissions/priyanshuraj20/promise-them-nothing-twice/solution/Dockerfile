# Lightweight Dockerfile for RelayAPI Rate Limiter Service
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy pre-packaged executable JAR
COPY target/*.jar app.jar

EXPOSE 8080

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring

ENTRYPOINT ["java", "-jar", "app.jar"]
