ALTER TABLE "app-vdyp"."projection" ADD COLUMN admin_cancel_reason VARCHAR(255);
COMMENT ON COLUMN "app-vdyp"."projection"."admin_cancel_reason"
	IS 'Admin Cancel Reason is a short, user-facing justification provided by an Administrator when cancelling a running projection.'
;
