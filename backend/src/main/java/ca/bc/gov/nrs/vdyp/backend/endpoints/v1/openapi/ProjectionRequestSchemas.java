package ca.bc.gov.nrs.vdyp.backend.endpoints.v1.openapi;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;

import ca.bc.gov.nrs.vdyp.backend.model.ModelParameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;

/**
 * Documentation-only multipart request schemas. RESTEasy binds the individual endpoint parameters directly; these
 * classes give OpenAPI clients and DAST tools the exact multipart field names and value types.
 */
public final class ProjectionRequestSchemas {

	private ProjectionRequestSchemas() {
	}

	@Schema(name = "DcsvProjectionRequest", description = "DCSV projection parameters and input data.")
	public static final class DcsvProjectionRequest {
		@JsonProperty(ParameterNames.PROJECTION_PARAMETERS)
		@Schema(description = "Projection execution parameters encoded as JSON.", required = true)
		public Parameters projectionParameters;

		@JsonProperty(ParameterNames.DCSV_INPUT_DATA)
		@Schema(description = "DCSV input file.", type = SchemaType.STRING, format = "binary", required = true)
		public String dcsvFile;
	}

	@Schema(name = "HcsvProjectionRequest", description = "HCSV projection parameters and input files.")
	public static final class HcsvProjectionRequest {
		@JsonProperty(ParameterNames.PROJECTION_PARAMETERS)
		@Schema(description = "Projection execution parameters encoded as JSON.", required = true)
		public Parameters projectionParameters;

		@JsonProperty(ParameterNames.HCSV_POLYGON_INPUT_DATA)
		@Schema(description = "HCSV polygon input file.", type = SchemaType.STRING, format = "binary", required = true)
		public String polygonFile;

		@JsonProperty(ParameterNames.HCSV_LAYERS_INPUT_DATA)
		@Schema(description = "HCSV layer input file.", type = SchemaType.STRING, format = "binary", required = true)
		public String layerFile;
	}

	@Schema(name = "ScsvProjectionRequest", description = "SCSV projection parameters and input files.")
	public static final class ScsvProjectionRequest {
		@JsonProperty(ParameterNames.PROJECTION_PARAMETERS)
		@Schema(description = "Projection execution parameters encoded as JSON.", required = true)
		public Parameters projectionParameters;

		@JsonProperty(ParameterNames.SCSV_POLYGON_INPUT_DATA)
		@Schema(description = "SCSV polygon input file.", type = SchemaType.STRING, format = "binary", required = true)
		public String polygonFile;

		@JsonProperty(ParameterNames.SCSV_LAYERS_INPUT_DATA)
		@Schema(description = "SCSV layer input file.", type = SchemaType.STRING, format = "binary", required = true)
		public String layerFile;

		@JsonProperty(ParameterNames.SCSV_HISTORY_INPUT_DATA)
		@Schema(description = "Optional SCSV history input file.", type = SchemaType.STRING, format = "binary")
		public String historyFile;

		@JsonProperty(ParameterNames.SCSV_NON_VEGETATION_INPUT_DATA)
		@Schema(description = "Optional SCSV non-vegetation input file.", type = SchemaType.STRING, format = "binary")
		public String nonVegetationFile;

		@JsonProperty(ParameterNames.SCSV_OTHER_VEGETATION_INPUT_DATA)
		@Schema(description = "Optional SCSV other-vegetation input file.", type = SchemaType.STRING, format = "binary")
		public String otherVegetationFile;

		@JsonProperty(ParameterNames.SCSV_POLYGON_ID_INPUT_DATA)
		@Schema(
				description = "Optional SCSV polygon identifier input file.", type = SchemaType.STRING, format = "binary"
		)
		public String polygonIdFile;

		@JsonProperty(ParameterNames.SCSV_SPECIES_INPUT_DATA)
		@Schema(description = "Optional SCSV species input file.", type = SchemaType.STRING, format = "binary")
		public String speciesFile;

		@JsonProperty(ParameterNames.SCSV_VRI_ADJUST_INPUT_DATA)
		@Schema(description = "Optional SCSV VRI adjustment input file.", type = SchemaType.STRING, format = "binary")
		public String vriAdjustFile;
	}

	@Schema(
			name = "ProjectionConfigurationRequest", description = "Projection, model, and report configuration supplied as multipart fields."
	)
	public static final class ProjectionConfigurationRequest {
		@JsonProperty(ParameterNames.PROJECTION_PARAMETERS)
		@Schema(description = "Projection execution parameters encoded as JSON.", required = true)
		public Parameters projectionParameters;

		@JsonProperty(ParameterNames.MODEL_PARAMETERS)
		@Schema(description = "Optional site-index model parameters encoded as JSON.")
		public ModelParameters modelParameters;

		@Schema(description = "Optional human-readable description included in the projection report.")
		public String reportDescription;
	}

	@Schema(name = "ProjectionFileUploadRequest", description = "A file to attach to a projection file set.")
	public static final class ProjectionFileUploadRequest {
		@Schema(description = "File content.", type = SchemaType.STRING, format = "binary", required = true)
		public String file;
	}
}
