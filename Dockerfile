# Stage 1: Build with Maven (has mvn pre-installed)
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Cache dependencies first
RUN mvn dependency:go-offline -B
# Build JAR
RUN mvn clean package -DskipTests

# Stage 2: Runtime (lightweight Java 17)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
