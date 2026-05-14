/* Increase model_parameters_json column size to accommodate per-species site index fields for species 2-6 */
ALTER TABLE "app-vdyp"."projection" ALTER COLUMN model_parameters_json TYPE varchar(5000);
COMMENT ON COLUMN "app-vdyp"."projection"."model_parameters_json"
	IS 'Model Parameters JSON is a JSON encoded string containing all parameters that relate exclusively to creating a polygon and layer model for an input model parameters projection.'
;
