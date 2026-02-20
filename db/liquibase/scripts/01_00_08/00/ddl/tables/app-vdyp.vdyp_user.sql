ALTER TABLE "app-vdyp"."vdyp_user"
	ADD COLUMN "display_name" varchar(250),    -- Display Name is the users name that should be displayed
	ADD COLUMN "email" varchar(255);    -- Email is the email of the user to send critical communications

UPDATE "app-vdyp"."vdyp_user"
SET "display_name" = ''
WHERE "display_name" IS NULL;
UPDATE "app-vdyp"."vdyp_user"
SET "email" = ''
WHERE "email" IS NULL;

ALTER TABLE "app-vdyp"."vdyp_user"
ALTER COLUMN "display_name" SET DEFAULT '',
ALTER COLUMN "display_name" SET NOT NULL,
ALTER COLUMN "email" SET DEFAULT '',
ALTER COLUMN "email" SET NOT NULL;

COMMENT ON COLUMN "app-vdyp"."vdyp_user"."display_name"
	IS 'Display Name is the users name that should be displayed'
;

COMMENT ON COLUMN "app-vdyp"."vdyp_user"."email"
	IS 'Email is the email of the user to send critical communications'
;
