# ============================================================
# DOCKERFILE - Shranvi Spring Boot Products API
# Multi-stage build for smaller, secure production image
# Port: 8081 (Flask uses 5000, avoid conflict)
# ============================================================

# ========================
# STAGE 1: BUILD STAGE
# Use Maven + JDK to compile and package the JAR
# ========================
FROM maven:3.9.5-eclipse-temurin-17-alpine AS builder

# Set working directory inside container
WORKDIR /app

# Copy pom.xml first (Docker caches this layer - speeds up builds)
COPY pom.xml .

# Download all Maven dependencies (cached if pom.xml hasn't changed)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application and run tests
# -DskipTests=false means tests WILL run during Docker build
RUN mvn clean package -DskipTests=false

# ========================
# STAGE 2: RUNTIME STAGE
# Only JRE needed (not full JDK) - smaller and more secure
# ========================
FROM eclipse-temurin:17-jre-alpine

# Security: Don't run as root
RUN addgroup -S shranvi && adduser -S shranvi -G shranvi

# Create log directory
RUN mkdir -p /var/log/shranvi-api && chown shranvi:shranvi /var/log/shranvi-api

# Set working directory
WORKDIR /app

# Copy the built JAR from Stage 1
COPY --from=builder /app/target/*.jar app.jar

# Change ownership
RUN chown shranvi:shranvi app.jar

# Switch to non-root user
USER shranvi

# Expose port 8081 (NOT 5000 - that's Flask, NOT 8080 - that's default Spring)
EXPOSE 8081

# Health check - Docker will automatically monitor your container
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/api/v1/products/health || exit 1

# JVM tuning for containers
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the Spring Boot application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
