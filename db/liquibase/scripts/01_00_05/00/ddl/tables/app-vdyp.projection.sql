/* Add more characters to the projection parameters json since it is currently very verbose*/
ALTER TABLE "app-vdyp"."projection" ALTER COLUMN projection_parameters_json TYPE varchar(3000);
/* Add model parameters json to store model specific parameters separate from projection parameters */
ALTER TABLE "app-vdyp"."projection" ADD COLUMN model_parameters_json VARCHAR(2000);
COMMENT ON COLUMN "app-vdyp"."projection"."model_parameters_json"
	IS 'Model Parameters JSON is a JSON encoded string containing all parameters that relate exclusively to creating a polygon and layer model for an input model parameters projection.'
;
COMMENT ON COLUMN "app-vdyp"."projection"."projection_parameters_json"
	IS 'Projection Parameters JSON is a JSON encoded string containing all parameters required to run this projection.  These parameters do not include any data that represents the model of the stand. That is provided either by a polygon and layer files, or model parameters JSON'
;
