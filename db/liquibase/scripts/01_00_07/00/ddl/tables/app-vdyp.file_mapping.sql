ALTER TABLE "app-vdyp"."file_mapping"
    ADD COLUMN "filename" VARCHAR(255);

UPDATE "app-vdyp"."file_mapping"
SET "filename" = ''
WHERE "filename" IS NULL;

ALTER TABLE "app-vdyp"."file_mapping"
ALTER COLUMN "filename" SET DEFAULT '',
ALTER COLUMN "filename" SET NOT NULL;
