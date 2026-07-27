ALTER TABLE "app-vdyp"."vdyp_user"
    ADD COLUMN IF NOT EXISTS "identity_provider_code" VARCHAR (10);

CREATE INDEX "user_idpcode_idx" ON "app-vdyp"."vdyp_user" ("identity_provider_code" ASC)
;

/* Create Foreign Key Constraints */

ALTER TABLE "app-vdyp"."vdyp_user"
    ADD CONSTRAINT "user_idpcode_fk"
        FOREIGN KEY ("identity_provider_code") REFERENCES "app-vdyp"."identity_provider_code" ("identity_provider_code") ON DELETE No Action ON UPDATE No Action
;

/* Create Table Comments, Sequences for Autonumber Columns */

COMMENT
ON COLUMN "app-vdyp"."vdyp_user"."identity_provider_code"
	IS 'identity_provider_code: Is a foreign key to identity_provider_code: Identity Provider Code is a code representing the identity provider used to authenticate a user profile. Values are: - IDIR - BCeID'
;
