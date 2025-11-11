# Spring Boot Batch Service Dockerfile - GraalVM Native Image
# Multi-stage build for VDYP Batch module

# ===========================
# Stage 1: Build Stage (GraalVM Native Image)
# ===========================
# Use GraalVM with Native Image support for building
FROM ghcr.io/graalvm/native-image-community:21 AS build

# Set working directory
WORKDIR /project

# ARG for build context (default: batch)
ARG CONTEXT="batch"

# Install Maven (GraalVM image doesn't include Maven by default)
RUN microdnf install -y findutils && \
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.tar.gz | tar xz -C /opt && \
    ln -s /opt/apache-maven-3.9.5/bin/mvn /usr/local/bin/mvn

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
# 4. Build batch module with GraalVM Native Image (-Pnative)
RUN ./buildtools/mvnw clean install -N --batch-mode && \
    cd buildtools && ./mvnw clean install --batch-mode -DskipTests && cd .. && \
    cd lib && ./mvnw clean install --batch-mode -DskipTests && cd .. && \
    cd ${CONTEXT} && ./mvnw clean package --batch-mode -DskipTests -Pnative

# ===========================
# Stage 2: Runtime Stage (Minimal Container)
# ===========================
# Use minimal base image for running native executable
FROM gcr.io/distroless/base-debian12:nonroot

# Set working directory
WORKDIR /app

# ARG for build context
ARG CONTEXT="batch"

# Copy the GraalVM native executable from build stage
# Native Maven Plugin creates vdyp-batch executable (no .jar extension)
COPY --from=build /project/${CONTEXT}/target/vdyp-batch /app/vdyp-batch

# Expose port 8081 (Spring Boot batch service port)
EXPOSE 8081

# Note: Distroless image doesn't have shell or wget for HEALTHCHECK
# Health checks will be handled by Kubernetes liveness/readiness probes

# Run the native executable
# - Native image doesn't need JVM, runs as standalone binary
# - Distroless runs as non-root user by default
ENTRYPOINT ["/app/vdyp-batch"]
