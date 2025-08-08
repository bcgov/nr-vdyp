#!/bin/sh
set -e
export PGPASSWORD="${POSTGRES_PASSWORD}"
psql -U postgres <<-EOSQL
  CREATE DATABASE coms;
  CREATE USER coms_local WITH ENCRYPTED PASSWORD 'password';
  GRANT ALL PRIVILEGES ON DATABASE coms TO coms_local;
EOSQL

psql -v ON_ERROR_STOP=1 -U postgres -d coms <<-EOSQL
  GRANT USAGE, CREATE ON SCHEMA public TO coms_local;
  ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL ON TABLES TO coms_local;
EOSQL

psql -U postgres <<-EOSQL
  CREATE DATABASE vdyp;
  CREATE USER vdyp_local WITH ENCRYPTED PASSWORD 'password';
  GRANT ALL PRIVILEGES ON DATABASE vdyp TO vdyp_local;
EOSQL

psql -v ON_ERROR_STOP=1 -U postgres -d vdyp <<-EOSQL
  GRANT USAGE, CREATE ON SCHEMA public TO vdyp_local;
  ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL ON TABLES TO vdyp_local;
EOSQL
psql -U postgres <<-EOSQL
  CREATE DATABASE batch;
  CREATE USER batch_local WITH ENCRYPTED PASSWORD 'password';
  GRANT ALL PRIVILEGES ON DATABASE batch TO batch_local;
EOSQL

psql -v ON_ERROR_STOP=1 -U postgres -d batch <<-EOSQL
  GRANT USAGE, CREATE ON SCHEMA public TO batch_local;
  ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL ON TABLES TO batch_local;
EOSQL
