# CLAUDE.md (db)

Guidance for work on the VDYP database schemas. Read the root `CLAUDE.md` first; this file adds database-specific detail.

## Purpose

`db/` holds everything that defines the PostgreSQL schemas used by `backend` and `batch`. All schema and reference data changes are delivered as Liquibase changesets from this folder. Neither service is allowed to create or alter its schema at runtime: the backend runs Hibernate with `schema-management.strategy=validate`, and the batch service sets `spring.batch.jdbc.initialize-schema=never`.

## Layout

| Path | Contents |
| --- | --- |
| `liquibase/postgres-changelog.json` | Admin changelog. Creates roles (`app_vdyp_custodian`, `app_vdyp_rest_proxy`), grants them to proxy users, and sets search paths. Runs as the PostgreSQL admin user, first. |
| `liquibase/vdyp-changelog.json` | Application schema `app-vdyp`, used by `backend`: projections, file sets, file mappings, users, code tables, and grants. |
| `liquibase/batch-changelog.json` | Schema `batch`, used by the `batch` service: Spring Batch tables, job claims and leases, worker registry. |
| `liquibase/scripts/<version>/<step>/...` | SQL files referenced by the changelogs. |
| `liquibase/Dockerfile` | Builds the `vdyp-liquibase` image (Liquibase plus the PostgreSQL client) containing the changelogs and scripts. |
| `eap/VDYP-LogicalModel.EAP` | Enterprise Architect logical data model. The original `01_00_00` table scripts were generated from it. |

Related locations outside this folder:

- `docker-compose.yml`: services `postgres-liquibase`, then `vdyp-liquibase` and `batch-liquibase`, run `update` on startup before the backend and batch services start.
- `charts/liquibase/`: Helm jobs (`job-liquibase-admin`, `job-liquibase`, `job-liquibase-batch`) that apply the three changelogs in OpenShift.
- `.github/workflows/liquibase-ddl.yml`: manual workflow that builds and pushes the image and deploys the Liquibase chart to `dev`, `test` or `prod`.
- `initdb/01_create_tables.sh`: local database initialization for Docker.
- `backend/src/main/resources/db/migration/V1.0.0__init.sql`: an older Flyway style script for a `java_api` schema. It is not part of the Liquibase flow; do not add new changes there.

## Versioning conventions

- Script folders use a single release sequence shared by all three changelogs: `scripts/01_00_NN/SS/`, where `NN` is the schema version and `SS` is a step within that version (`00`, `01`, ...). One version folder can contain changes for more than one changelog (for example `01_00_19` has both `app-vdyp` and `batch` scripts).
- Changeset `id` values follow the folder: `01_00_NN_SS`. Use the next unused `NN` across the whole `scripts/` folder, not just within one changelog.
- `vdyp-changelog.json` and `postgres-changelog.json` changesets begin with a `tagDatabase` change tagged `version_01_00_NN_SS`. Follow the same pattern for new changesets in those files.
- Within a step folder, scripts are grouped as `ddl/tables/`, `ddl/grants/` or `grants/`, `ddl/roles/`, and `dml/`.
- File names are `<schema>.<object>.sql` for the application schema (for example `app-vdyp.file_mapping.sql`) and descriptive names for batch scripts (for example `batch-job-claim.sql`, with a matching `batch-job-claim-drop.sql` or `...-rollback.sql`).
- Each SQL file is referenced through an `sqlFile` change with `"dbms": "postgresql"`, `"endDelimiter": ";"` and `"relativeToChangelogFile": true`. Every script in `scripts/` is currently referenced by a changelog; keep it that way.
- The changelog JSON files use two-space indentation (an exception recorded in `.editorconfig`). Changelogs set `objectQuotingStrategy` to `QUOTE_ALL_OBJECTS`.

## SQL style

- Always quote and schema-qualify identifiers: `"app-vdyp"."file_mapping"`. The schema name contains a hyphen, so it must be quoted.
- Table and column names are lower snake case. Primary keys are UUID columns named `<table>_guid`. Code tables are named `<name>_code` and have `description`, `display_order`, `effective_date` and `expiry_date` columns.
- Tables carry the audit columns `revision_count` (optimistic locking), `create_user`, `create_date`, `update_user` and `update_date`.
- Add a `COMMENT ON TABLE` or `COMMENT ON COLUMN` for every new table and column. Comments document the data model and are expected in review.
- Make changes idempotent where PostgreSQL allows it (`ADD COLUMN IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`).
- The original `01_00_00` table scripts begin with `DROP TABLE IF EXISTS ... CASCADE`. Do not copy that pattern into new changesets; alter existing tables instead.

## Rules for changes

- Never edit a changeset or a script that has already been released. Liquibase checksums will fail, and deployed databases will diverge. Add a new changeset in a new version folder instead.
- Provide a `rollback` for every new changeset: either inline `sql` or an `sqlFile` pointing to a matching drop or rollback script.
- New tables in `app-vdyp` need grants for `app_vdyp_rest_proxy`. Add a grants script in the same version (see `01_00_15/01/grants/` for an example).
- Reference data changes (new code values) go in `dml/` scripts with explicit columns, and the rollback deletes exactly the rows inserted.
- Keep the JPA entities in `backend/.../data/entities` and the batch `persistence/model` classes in step with the schema. Because Hibernate validates the schema, a mismatch prevents the backend from starting.
- Update the logical model in `eap/` when a change alters the data model, or note in the pull request that it needs updating.
- Test a change locally by rebuilding and running the Liquibase services: `docker compose up --build postgres-liquibase vdyp-liquibase batch-liquibase`. Then start the service that uses the schema and confirm it starts and its tests pass.
- Production deployment of schema changes follows `docs/deployment-runbook.md` and the `liquibase-ddl.yml` workflow. Do not run Liquibase against shared environments from a local machine.
