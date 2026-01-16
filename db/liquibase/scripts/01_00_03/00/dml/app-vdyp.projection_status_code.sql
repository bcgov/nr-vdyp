INSERT INTO "app-vdyp".projection_status_code (projection_status_code,description,display_order,effective_date,expiry_date,revision_count,create_user,create_date,update_user,update_date) VALUES
	 ('FAILED','Failed',4,'2025-11-28','9999-12-31',1,'System','2025-11-28','System','2025-11-28');
UPDATE "app-vdyp".projection_status_code SET projection_status_code = 'READY', description = 'Ready' WHERE projection_status_code = 'COMPLETE';
UPDATE "app-vdyp".projection_status_code SET projection_status_code = 'RUNNING', description = 'Running' WHERE projection_status_code = 'INPROGRESS';
