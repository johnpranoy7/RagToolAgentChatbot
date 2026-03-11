
FROM eclipse-temurin:17-jre-alpine

# Create app directory
WORKDIR /app

# Copy Maven-built JAR (Railway runs `mvn package` first)
COPY target/*.jar app.jar

# Expose port (Railway maps to $PORT)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
