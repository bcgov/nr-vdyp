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
# Use Oracle Linux Slim with GraalVM native libraries
# This image includes necessary shared libraries (libz, glibc, etc.) for native executables
FROM container-registry.oracle.com/os/oraclelinux:9-slim

# Install only essential runtime dependencies and create non-root user
RUN microdnf install -y zlib glibc-langpack-en && \
    microdnf clean all && \
    rm -rf /var/cache/yum && \
    useradd -r -u 1001 -g 0 appuser

# Set working directory
WORKDIR /app

# ARG for build context
ARG CONTEXT="batch"

# Copy the GraalVM native executable from build stage
# Native Maven Plugin creates vdyp-batch executable (no .jar extension)
COPY --from=build /project/${CONTEXT}/target/vdyp-batch /app/vdyp-batch

# Create work directory for batch processing
RUN mkdir -p /app/work && \
    chown -R 1001:0 /app && \
    chmod +x /app/vdyp-batch && \
    chmod -R g=u /app

# Switch to non-root user
USER 1001

# Expose port 8081 (Spring Boot batch service port)
EXPOSE 8081

# Set batch root directory to writable location
ENV BATCH_ROOT_DIRECTORY=/app/work

# Run the native executable
# - Native image doesn't need JVM, runs as standalone binary
# - Runs as non-root user (1001)
ENTRYPOINT ["/app/vdyp-batch"]
