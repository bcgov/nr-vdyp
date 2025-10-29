FROM liquibase/liquibase:5.0.1

COPY ./scripts ./scripts
COPY ./vdyp-changelog.json .
COPY ./postgres-changelog.json .

# Install Postgres client utilities
USER root
RUN lpm add postgresql --global
RUN apt-get update && apt-get install -y postgresql-client && rm -rf /var/lib/apt/lists/*

user liquibase
