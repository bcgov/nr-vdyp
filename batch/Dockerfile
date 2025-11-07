# Spring Boot Batch Service Dockerfile
# Multi-stage build for VDYP Batch module

# ===========================
# Stage 1: Build Stage
# ===========================
# Use Maven + Eclipse Temurin JDK 17 for building
FROM maven:3.9-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /project

# ARG for build context (default: batch)
ARG CONTEXT="batch"

# Copy project files (controlled by .dockerignore)
# Note: We need parent pom.xml, buildtools, lib dependencies, and batch module
COPY ./pom.xml ./pom.xml
COPY ./buildtools ./buildtools
COPY ./lib ./lib
COPY ./${CONTEXT} ./${CONTEXT}

# Convert CRLF to LF and make Maven wrapper executable
RUN find . -type f -name "mvnw" -exec sed -i 's/\r$//' {} \; && \
    find . -type f -name "*.properties" -path "*/.mvn/wrapper/*" -exec sed -i 's/\r$//' {} \; && \
    chmod +x buildtools/mvnw lib/mvnw ${CONTEXT}/mvnw

# Build dependencies in order:
# 1. Install parent pom
# 2. Build and install buildtools (formatter, etc.)
# 3. Build and install lib modules (vdyp-common, vdyp-extended-core, etc.)
# 4. Build batch module with Spring Boot repackage plugin
RUN ./buildtools/mvnw clean install -N --batch-mode && \
    cd buildtools && ./mvnw clean install --batch-mode -DskipTests && cd .. && \
    cd lib && ./mvnw clean install --batch-mode -DskipTests && cd .. && \
    cd ${CONTEXT} && ./mvnw clean package --batch-mode -DskipTests

# ===========================
# Stage 2: Runtime Stage
# ===========================
# Use lightweight Eclipse Temurin JRE 17 for running
FROM eclipse-temurin:17-jre-alpine

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# ARG for build context
ARG CONTEXT="batch"

# Copy the Spring Boot fat JAR from build stage
# Spring Boot maven plugin creates vdyp-batch-8.0.0-SNAPSHOT.jar
COPY --chown=spring:spring --from=build /project/${CONTEXT}/target/vdyp-batch-*.jar /app/app.jar

# Switch to non-root user
USER spring:spring

# Expose port 8081 (Spring Boot batch service port)
EXPOSE 8081

# Health check for Spring Boot Actuator
# Note: context-path is /vdyp-batch, so health endpoint is at /vdyp-batch/actuator/health
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/vdyp-batch/actuator/health || exit 1

# Run the Spring Boot application
# - Use exec form for proper signal handling
# - Spring Boot executable JAR includes embedded Tomcat
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
