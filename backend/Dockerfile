# Quarkus Images
# https://github.com/quarkusio/quarkus-images

# "Provides the native-image executable. Used by the Maven and Gradle plugin from Quarkus to build linux64 executables"
FROM quay.io/quarkus/ubi-quarkus-graalvmce-builder-image:jdk-21 AS build
ARG CONTEXT="backend"

# Image defaults to /project; copy controlled by .dockerignore
COPY --chown=quarkus:quarkus ./${CONTEXT} ./${CONTEXT}
COPY --chown=quarkus:quarkus ./buildtools ./buildtools
COPY ./pom.xml ./pom.xml
RUN chmod +x buildtools/mvnw && chmod +x ${CONTEXT}/mvnw && ./buildtools/mvnw clean install -N --batch-mode
RUN cd buildtools && ./mvnw clean install --batch-mode -DskipTests && cd ..
RUN cd ${CONTEXT} && ./mvnw clean package --batch-mode -DskipTests -Pnative

# "A base image to run Quarkus native application using UBI Micro"
FROM quay.io/quarkus/quarkus-micro-image:2.0

# Port and health check
EXPOSE 8080
HEALTHCHECK --interval=300s --timeout=3s CMD curl -f http://localhost:8080/ || exit 1

# Startup
ARG CONTEXT="./backend"
COPY --chown=1001:root --from=build /project/${CONTEXT}/target/*-runner /app
USER 1001
CMD ["/app", "-Dquarkus.http.host=0.0.0.0"]
