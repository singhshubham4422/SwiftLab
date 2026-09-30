# ==============================================================================
# SwiftLab Inventory & Billing Platform - Docker Container Build
# Multi-stage build for Render / Cloud deployment (Java 21)
# Build Context: SwiftLab repository root
# ==============================================================================

# Build Stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache dependencies
COPY Invoice-Billing-Application/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build executable JAR
COPY Invoice-Billing-Application/src ./src
RUN mvn clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create unprivileged runtime user
RUN addgroup -S swiftlab && adduser -S swiftlab -G swiftlab

# Copy artifact from builder
COPY --from=builder /app/target/invoice-app-1.0.0.jar app.jar

# Configuration defaults
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=cloud
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

EXPOSE 8080

USER swiftlab

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
