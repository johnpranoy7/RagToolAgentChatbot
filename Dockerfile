# Stage 1: Build JAR with Maven
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Download dependencies first (cache layer)
RUN mvn dependency:go-offline -B
# Build JAR
RUN mvn clean package -DskipTests

# Stage 2: Runtime (lightweight)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
