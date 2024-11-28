package ca.bc.gov.nrs.vdyp.backend.v1.gen.api;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import ca.bc.gov.nrs.vdyp.backend.v1.api.ProjectionService;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.Parameters;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v8/projection")
@io.swagger.annotations.Api(description = "the projection API")
@RegisterForReflection
public class ProjectionEndpoint implements Endpoint {

	@Inject
	private ProjectionService projectionService;

	@jakarta.ws.rs.POST
	@Path("/dcsv")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON })
	@Tag(
			name = "Run DCSV Projection", description = "Run a projection of polygons in the supplied DCSV formatted input file as controlled by the parameters in the supplied projection parameters file."
	)
	public Response projectionDcsvPost(
			@QueryParam(value = ParameterNames.TRIAL_RUN) @DefaultValue("false") Boolean trialRun, //
			@RestForm(value = ParameterNames.PROJECTION_PARAMETERS) @PartType(
				MediaType.APPLICATION_JSON
			) Parameters parameters, //
			@FormParam(value = ParameterNames.DCSV_INPUT_DATA) FileUpload dcsvDataStream //
			/* , @Context SecurityContext securityContext */

	) {
		return projectionService.projectionDcsvPost(
				parameters, dcsvDataStream, true /* trialRun */, null /* securityContext */
		);
	}

	@jakarta.ws.rs.POST
	@Path("/hcsv")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON })
	@Tag(
			name = "Run HCSV Projection", description = "Run a projection of polygons in the supplied polygon and layers input files as controlled by the parameters in the supplied projection parameters file."
	)
	public Response projectionHcsvPost(
			@QueryParam(value = ParameterNames.TRIAL_RUN) @DefaultValue("false") Boolean trialRun, //
			@RestForm(value = ParameterNames.PROJECTION_PARAMETERS) @PartType(
				MediaType.APPLICATION_JSON
			) Parameters parameters, //
			@FormParam(value = ParameterNames.POLYGON_INPUT_DATA) FileUpload polygonDataStream, //
			@FormParam(value = ParameterNames.LAYERS_INPUT_DATA) FileUpload layersDataStream //
			// , @Context SecurityContext securityContext
	) {
		return projectionService.projectionHcsvPost(
				trialRun, parameters, polygonDataStream, layersDataStream, null /* securityContext */
		);
	}

	@jakarta.ws.rs.POST
	@Path("/scsv")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON })
	@Tag(
			name = "Run SCSV Projection", description = "Run a projection of polygons in the supplied SCSV input files as controlled by the parameters in the supplied projection parameters file."
	)
	public Response projectionScsvPost(
			@QueryParam(value = ParameterNames.TRIAL_RUN) @DefaultValue("false") Boolean trialRun, //
			@RestForm(value = ParameterNames.PROJECTION_PARAMETERS) @PartType(
				MediaType.APPLICATION_JSON
			) Parameters parameters, //
			@FormParam(value = ParameterNames.POLYGON_INPUT_DATA) FileUpload polygonDataStream, //
			@FormParam(value = ParameterNames.LAYERS_INPUT_DATA) FileUpload layersDataStream, //
			@FormParam(value = ParameterNames.HISTORY_INPUT_DATA) FileUpload historyDataStream, //
			@FormParam(value = ParameterNames.NON_VEGETATION_INPUT_DATA) FileUpload nonVegetationDataStream, //
			@FormParam(value = ParameterNames.OTHER_VEGETATION_INPUT_DATA) FileUpload otherVegetationDataStream, //
			@FormParam(value = ParameterNames.POLYGON_ID_INPUT_DATA) FileUpload polygonIdDataStream, //
			@FormParam(value = ParameterNames.SPECIES_INPUT_DATA) FileUpload speciesDataStream, //
			@FormParam(value = ParameterNames.VRI_ADJUST_INPUT_DATA) FileUpload vriAdjustDataStream //
			// , @Context SecurityContext securityContext
	) {
		return projectionService
				.projectionScsvPost(
						trialRun, parameters, polygonDataStream, layersDataStream, historyDataStream,
						nonVegetationDataStream, otherVegetationDataStream, polygonIdDataStream, speciesDataStream,
						vriAdjustDataStream, null /* securityContext */
				);
	}
}
