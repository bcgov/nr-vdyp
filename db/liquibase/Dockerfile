FROM liquibase/liquibase:4.30.0

COPY ./scripts ./scripts
COPY ./vdyp-changelog.json .
COPY ./postgres-changelog.json .

# Install Postgres client utilities
USER root
RUN apt-get update && apt-get install -y postgresql-client && rm -rf /var/lib/apt/lists/*

user liquibase
