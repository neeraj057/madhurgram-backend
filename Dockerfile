# Stage 1: Build stage using Maven and JDK 21 Alpine
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache dependencies by copying pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package jar (skipping test suites for speed)
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Minimalist runtime container using JRE 21 Alpine
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as a non-privileged system user for enhanced security
RUN addgroup -S spring && adduser -S spring -G spring

# Create log directory and change ownership of the workdir
RUN mkdir -p /app/logs && chown -R spring:spring /app

# Copy built jar from the builder stage with ownership assigned to the spring user
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# Expose standard application port
EXPOSE 8080

# Run JVM in container-aware mode
USER spring:spring
ENTRYPOINT ["java", "-jar", "app.jar"]
