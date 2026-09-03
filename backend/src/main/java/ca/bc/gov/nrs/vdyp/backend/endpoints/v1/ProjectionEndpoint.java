package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.backend.context.CurrentVDYPUser;
import ca.bc.gov.nrs.vdyp.backend.data.models.BatchStorageStatusModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.BatchThreadCapacityModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionModel;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.impl.Endpoint;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers.ApiError;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.openapi.ProjectionRequestSchemas.DcsvProjectionRequest;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.openapi.ProjectionRequestSchemas.HcsvProjectionRequest;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.openapi.ProjectionRequestSchemas.ProjectionConfigurationRequest;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.openapi.ProjectionRequestSchemas.ProjectionFileUploadRequest;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.openapi.ProjectionRequestSchemas.ScsvProjectionRequest;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionServiceException;
import ca.bc.gov.nrs.vdyp.backend.model.CancelProjectionRequest;
import ca.bc.gov.nrs.vdyp.backend.model.ModelParameters;
import ca.bc.gov.nrs.vdyp.backend.model.ProjectionProgressUpdate;
import ca.bc.gov.nrs.vdyp.backend.services.ProjectionService;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;

@Path("/api/v8/projection")
@Tag(name = "Projections", description = "Create, configure, execute, monitor, and retrieve VDYP projections.")
@APIResponse(responseCode = "401", description = "A valid bearer token is required.")
@APIResponse(
		responseCode = "403", description = "The authenticated identity does not have the permission to access this projection."
)
@APIResponse(
		responseCode = "500", description = "An unexpected service error occurred.", content = @Content(
				mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)
		)
)
@RegisterForReflection
@ApplicationScoped
public class ProjectionEndpoint implements Endpoint {

	public static final Logger logger = LoggerFactory.getLogger(ProjectionEndpoint.class);
	private final ProjectionService projectionService;

	private final CurrentVDYPUser currentUser;

	private static final ObjectMapper mapper = new ObjectMapper();

	private final Client client;

	@Inject
	public ProjectionEndpoint(ProjectionService service, CurrentVDYPUser currentUser) {
		this(service, currentUser, ClientBuilder.newBuilder().build());
	}

	ProjectionEndpoint(ProjectionService service, CurrentVDYPUser currentUser, Client client) {
		this.projectionService = service;
		this.currentUser = currentUser;
		this.client = client;
	}

	@jakarta.ws.rs.POST
	@Path("/dcsv")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "runDcsvProjection", summary = "Run a DCSV projection", description = "Runs a projection using a DCSV input file and the supplied projection parameters. This operation is not currently implemented."
	)
	@RequestBody(
			required = true, description = "DCSV projection parameters and input data.", content = @Content(
					mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(
							implementation = DcsvProjectionRequest.class
					)
			)
	)
	@APIResponse(responseCode = "501", description = "DCSV projection processing is not implemented.")
	public Response projectionDcsvPost(
			@Parameter(
					description = "Run validation without retaining projection output.", example = "false"
			) @QueryParam(value = ParameterNames.TRIAL_RUN) @DefaultValue("false") Boolean trialRun, //
			@RestForm(value = ParameterNames.PROJECTION_PARAMETERS) @PartType(
				MediaType.APPLICATION_JSON
			) Parameters parameters, //
			@FormParam(value = ParameterNames.DCSV_INPUT_DATA) FileUpload dcsvDataStream //
			/* , @Context SecurityContext securityContext */

	) {
		try {
			return projectionService.projectionDcsvPost(
					parameters, dcsvDataStream, true /* trialRun */, null /* securityContext */
			);
		} catch (ProjectionRequestValidationException e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getValidationMessages()).build();
		} catch (PolygonExecutionException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
		}
	}

	@jakarta.ws.rs.POST
	@Path("/hcsv")
	@RolesAllowed({ "USER", "ADMIN", "KONG_API_GATEWAY" })
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "runHcsvProjection", summary = "Run an HCSV projection", description = "Runs a projection using HCSV polygon and layer files and returns a ZIP archive of projection output."
	)
	@RequestBody(
			required = true, description = "HCSV projection parameters and input files.", content = @Content(
					mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(
							implementation = HcsvProjectionRequest.class
					)
			)
	)
	@APIResponse(
			responseCode = "200", description = "Projection output ZIP archive.", content = @Content(
					mediaType = MediaType.APPLICATION_OCTET_STREAM, schema = @Schema(
							type = SchemaType.STRING, format = "binary"
					)
			)
	)
	@APIResponse(
			responseCode = "400", description = "The parameters or input files failed validation.", content = @Content(
					mediaType = MediaType.APPLICATION_JSON, schema = @Schema(
							implementation = ValidationMessageListResource.class
					)
			)
	)
	public Response projectionHcsvPost(
			@Parameter(
					description = "Run validation without retaining projection output.", example = "false"
			) @QueryParam(value = ParameterNames.TRIAL_RUN) @DefaultValue("false") Boolean trialRun, //
			@RestForm(value = ParameterNames.PROJECTION_PARAMETERS) @PartType(
				MediaType.APPLICATION_JSON
			) Parameters parameters, //
			@FormParam(value = ParameterNames.HCSV_POLYGON_INPUT_DATA) FileUpload polygonDataStream, //
			@FormParam(value = ParameterNames.HCSV_LAYERS_INPUT_DATA) FileUpload layersDataStream //
			// , @Context SecurityContext securityContext
	) {
		if (polygonDataStream == null) {
			return Response.status(Status.BAD_REQUEST).entity("Projection request failed: no polygon data supplied")
					.build();
		}
		if (layersDataStream == null) {
			return Response.status(Status.BAD_REQUEST).entity("Projection request failed: no layer data supplied")
					.build();
		}

		var polygonFile = polygonDataStream.uploadedFile();
		var layerFile = layersDataStream.uploadedFile();

		try {
			return projectionService.projectionHcsvPost(
					trialRun, parameters, polygonFile, layerFile, null /* securityContext */
			);
		} catch (ProjectionRequestValidationException e) {
			try {
				return Response.status(Status.BAD_REQUEST).header("content-type", "application/json")
						.entity(serialize(new ValidationMessageListResource(e.getValidationMessages()))).build();
			} catch (JsonProcessingException serializationException) {
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(
								serializationException.getMessage() == null ? "unknown reason"
										: serializationException.getMessage()
						).build();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(e.getMessage() == null ? "unknown reason" : e.getMessage()).build();
		}
	}

	@jakarta.ws.rs.POST
	@Path("/scsv")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON })
	@SecurityRequirements
	@Operation(
			operationId = "runScsvProjection", summary = "Run an SCSV projection", description = "Runs a projection using SCSV input files and the supplied projection parameters. This operation is not currently implemented."
	)
	@RequestBody(
			required = true, description = "SCSV projection parameters and input files.", content = @Content(
					mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(
							implementation = ScsvProjectionRequest.class
					)
			)
	)
	@APIResponse(responseCode = "501", description = "SCSV projection processing is not implemented.")
	public Response projectionScsvPost(
			@Parameter(
					description = "Run validation without retaining projection output.", example = "false"
			) @QueryParam(value = ParameterNames.TRIAL_RUN) @DefaultValue("false") Boolean trialRun, //
			@RestForm(value = ParameterNames.PROJECTION_PARAMETERS) @PartType(
				MediaType.APPLICATION_JSON
			) Parameters parameters, //
			@FormParam(value = ParameterNames.SCSV_POLYGON_INPUT_DATA) FileUpload polygonDataStream, //
			@FormParam(value = ParameterNames.SCSV_LAYERS_INPUT_DATA) FileUpload layersDataStream, //
			@FormParam(value = ParameterNames.SCSV_HISTORY_INPUT_DATA) FileUpload historyDataStream, //
			@FormParam(value = ParameterNames.SCSV_NON_VEGETATION_INPUT_DATA) FileUpload nonVegetationDataStream, //
			@FormParam(value = ParameterNames.SCSV_OTHER_VEGETATION_INPUT_DATA) FileUpload otherVegetationDataStream, //
			@FormParam(value = ParameterNames.SCSV_POLYGON_ID_INPUT_DATA) FileUpload polygonIdDataStream, //
			@FormParam(value = ParameterNames.SCSV_SPECIES_INPUT_DATA) FileUpload speciesDataStream, //
			@FormParam(value = ParameterNames.SCSV_VRI_ADJUST_INPUT_DATA) FileUpload vriAdjustDataStream //
			// , @Context SecurityContext securityContext
	) {
		try {
			return projectionService.projectionScsvPost(
					trialRun, parameters, polygonDataStream, layersDataStream, historyDataStream,
					nonVegetationDataStream, otherVegetationDataStream, polygonIdDataStream, speciesDataStream,
					vriAdjustDataStream, null /* securityContext */
			);
		} catch (ProjectionRequestValidationException e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getValidationMessages()).build();
		} catch (PolygonExecutionException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
		}
	}

	@GET
	@RolesAllowed({ "USER", "ADMIN" })
	@Path("/me")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "getCurrentUserProjections", summary = "List the current user's projections", description = "Returns every projection owned by the authenticated user."
	)
	@APIResponse(
			responseCode = "200", description = "The current user's projections.", content = @Content(
					mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProjectionModel[].class)
			)
	)
	public Response getAuthenticatedUserProjections() {
		var projections = projectionService.getAllProjectionsForUser(currentUser.getUserId());
		return Response.ok(projections).status(Response.Status.OK).build();
	}

	@GET
	@RolesAllowed("ADMIN")
	@Path("/all")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "getAllRunningProjections", summary = "List all running projections", description = "Returns all running projections across all users. Requires the ADMIN role."
	)
	@APIResponse(
			responseCode = "200", description = "All running projections.", content = @Content(
					mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProjectionModel[].class)
			)
	)
	public Response getAllRunningProjections() {
		var projections = projectionService.getAllRunningProjections();
		return Response.ok(projections).status(Response.Status.OK).build();
	}

	@GET
	@RolesAllowed("ADMIN")
	@Path("/thread-capacity")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "getBatchThreadCapacity", summary = "Get batch thread capacity", description = "Returns the batch service's configured worker capacity. Requires the ADMIN role."
	)
	@APIResponse(
			responseCode = "200", description = "Batch worker capacity.", content = @Content(
					mediaType = MediaType.APPLICATION_JSON, schema = @Schema(
							implementation = BatchThreadCapacityModel.class
					)
			)
	)
	public Response getThreadCapacity() {
		var threadCapacity = new BatchThreadCapacityModel(projectionService.getThreadCapacity());
		return Response.ok(threadCapacity).status(Response.Status.OK).build();
	}

	@GET
	@RolesAllowed("ADMIN")
	@Path("/storage-status")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "getBatchStorageStatus", summary = "Get batch storage status", description = "Returns batch-service persistent-storage utilization and threshold status. Requires the ADMIN role."
	)
	@APIResponse(
			responseCode = "200", description = "Batch storage utilization.", content = @Content(
					mediaType = MediaType.APPLICATION_JSON, schema = @Schema(
							implementation = BatchStorageStatusModel.class
					)
			)
	)
	public Response getStorageStatus() {
		BatchStorageStatusModel storageStatus = projectionService.getStorageStatus();
		return Response.ok(storageStatus).status(Response.Status.OK).build();
	}

	@POST
	@RolesAllowed({ "USER", "ADMIN" })
	@Path("/new")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "createProjection", summary = "Create a projection", description = "Creates an empty projection owned by the authenticated user using the supplied configuration."
	)
	@RequestBody(
			required = true, description = "Initial projection configuration.", content = @Content(
					mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(
							implementation = ProjectionConfigurationRequest.class
					)
			)
	)
	@APIResponse(
			responseCode = "201", description = "The projection was created.", content = @Content(
					mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProjectionModel.class)
			)
	)
	public Response createEmptyProjection(
			@RestForm(value = ParameterNames.PROJECTION_PARAMETERS) @PartType(
				MediaType.APPLICATION_JSON
			) Parameters parameters,
			@RestForm(value = ParameterNames.MODEL_PARAMETERS) @PartType(
				MediaType.APPLICATION_JSON
			) ModelParameters modelParameters, //
			@RestForm("reportDescription") String reportDescription
	) throws ProjectionServiceException {
		var created = projectionService
				.createNewProjection(currentUser.getUser(), parameters, modelParameters, reportDescription);
		return Response.status(Status.CREATED).entity(created).build();
	}

	@GET
	@RolesAllowed({ "USER", "ADMIN", "SYSTEM" })
	@Path("/{projectionGUID}")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "getProjection", summary = "Get a projection", description = "Returns the configuration, status, ownership, and file-set details for a projection."
	)
	@APIResponse(
			responseCode = "200", description = "Projection details.", content = @Content(
					mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProjectionModel.class)
			)
	)
	@APIResponse(
			responseCode = "404", description = "The projection does not exist.", content = @Content(
					mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)
			)
	)
	public Response getProjection(
			@Parameter(description = "Projection identifier.", required = true) @PathParam(
				"projectionGUID"
			) UUID projectionGUID
	) throws ProjectionServiceException {
		var fetched = projectionService.getProjectionByID(projectionGUID, currentUser.getUser());
		return Response.status(Status.OK).entity(fetched).build();
	}

	@PUT
	@RolesAllowed({ "USER", "ADMIN" })
	@Path("/{projectionGUID}/params")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "replaceProjectionParameters", summary = "Replace projection parameters", description = "Replaces the complete projection, model, and report configuration. Values are not merged with the existing configuration."
	)
	@RequestBody(
			required = true, description = "Replacement projection configuration.", content = @Content(
					mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(
							implementation = ProjectionConfigurationRequest.class
					)
			)
	)
	@APIResponse(
			responseCode = "200", description = "The updated projection.", content = @Content(
					mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProjectionModel.class)
			)
	)
	public Response editProjectionParameters(
			@Parameter(description = "Projection identifier.", required = true) @PathParam(
				"projectionGUID"
			) UUID projectionGUID,
			@RestForm(value = ParameterNames.PROJECTION_PARAMETERS) @PartType(
				MediaType.APPLICATION_JSON
			) Parameters parameters,
			@RestForm(value = ParameterNames.MODEL_PARAMETERS) @PartType(
				MediaType.APPLICATION_JSON
			) ModelParameters modelParameters, @RestForm("reportDescription") String reportDescription
	) throws ProjectionServiceException {
		var created = projectionService.editProjectionParameters(
				projectionGUID, parameters, modelParameters, reportDescription, currentUser.getUser()
		);
		return Response.status(Status.OK).entity(created).build();
	}

	@DELETE
	@RolesAllowed({ "USER", "ADMIN" })
	@Path("/{projectionGUID}")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "deleteProjection", summary = "Delete a projection", description = "Deletes a projection owned by the authenticated user."
	)
	@APIResponse(responseCode = "204", description = "The projection was deleted.")
	public Response deleteProjection(
			@Parameter(description = "Projection identifier.", required = true) @PathParam(
				"projectionGUID"
			) UUID projectionGUID
	) throws ProjectionServiceException {
		projectionService.deleteProjection(projectionGUID, currentUser.getUser());
		return Response.status(Status.NO_CONTENT).build();
	}

	@POST
	@RolesAllowed({ "USER", "ADMIN" })
	@Path("/{projectionGUID}/run")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "runProjection", summary = "Run a projection", description = "Queues a configured projection for batch processing."
	)
	@APIResponse(
			responseCode = "200", description = "The queued projection.", content = @Content(
					mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProjectionModel.class)
			)
	)
	public Response runProjection(
			@Parameter(description = "Projection identifier.", required = true) @PathParam(
				"projectionGUID"
			) UUID projectionGUID
	) throws ProjectionServiceException {
		var started = projectionService.queueForBatchProjection(currentUser.getUser(), projectionGUID);
		return Response.status(Status.OK).entity(started).build();
	}

	@PATCH
	@RolesAllowed("SYSTEM")
	@Path("/{projectionGUID}/progress")
	@Produces({ MediaType.APPLICATION_JSON })
	@Consumes({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "updateProjectionProgress", summary = "Update projection progress", description = "Updates processing progress for a running projection. Requires the SYSTEM role."
	)
	@APIResponse(responseCode = "200", description = "The progress update was accepted.")
	public Response updateCompleteProjectionProgress(
			@Parameter(description = "Projection identifier.", required = true) @PathParam(
				"projectionGUID"
			) UUID projectionGUID,
			@RequestBody(
					required = true, description = "Current batch progress.", content = @Content(
							mediaType = MediaType.APPLICATION_JSON, schema = @Schema(
									implementation = ProjectionProgressUpdate.class
							)
					)
			) ProjectionProgressUpdate progressUpdate
	) throws ProjectionServiceException {
		projectionService.updateProgress(currentUser.getUser(), projectionGUID, progressUpdate);
		return Response.status(Status.OK).build();
	}

	@POST
	@RolesAllowed("SYSTEM")
	@Path("/{projectionGUID}/complete")
	@Produces({ MediaType.APPLICATION_JSON })
	@Consumes({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "completeProjection", summary = "Complete a projection", description = "Records final processing metrics and marks a projection complete or failed. Requires the SYSTEM role."
	)
	@APIResponse(
			responseCode = "200", description = "The completed projection.", content = @Content(
					mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProjectionModel.class)
			)
	)
	public Response updateCompleteProjectionStatus(
			@Parameter(description = "Projection identifier.", required = true) @PathParam(
				"projectionGUID"
			) UUID projectionGUID,
			@Parameter(
					description = "Whether processing completed successfully.", required = true, example = "true"
			) @QueryParam("success") boolean success,
			@RequestBody(
					required = true, description = "Final batch progress and failure details.", content = @Content(
							mediaType = MediaType.APPLICATION_JSON, schema = @Schema(
									implementation = ProjectionProgressUpdate.class
							)
					)
			) ProjectionProgressUpdate progressUpdate
	) throws ProjectionServiceException {
		var started = projectionService
				.updateCompleteStatus(currentUser.getUser(), projectionGUID, success, progressUpdate);
		return Response.status(Status.OK).entity(started).build();
	}

	@POST
	@RolesAllowed("ADMIN")
	@Path("/{projectionGUID}/prioritize")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "prioritizeProjection", summary = "Prioritize a projection", description = "Moves a running projection ahead in the batch queue. Requires the ADMIN role."
	)
	@APIResponse(
			responseCode = "200", description = "The prioritized projection.", content = @Content(
					mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProjectionModel.class)
			)
	)
	public Response prioritizeProjection(
			@Parameter(description = "Projection identifier.", required = true) @PathParam(
				"projectionGUID"
			) UUID projectionGUID
	) throws ProjectionServiceException {
		var prioritized = projectionService.prioritizeBatchProjection(currentUser.getUser(), projectionGUID);
		return Response.status(Status.OK).entity(prioritized).build();
	}

	private <T> String serialize(T entity) throws JsonProcessingException {
		return mapper.writeValueAsString(entity);
	}

	@POST
	@RolesAllowed({ "USER", "ADMIN" })
	@Path("/{projectionGUID}/cancel")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "cancelProjection", summary = "Cancel a projection", description = "Cancels a queued or running projection. Administrators may provide a cancellation reason."
	)
	@APIResponse(
			responseCode = "200", description = "The cancelled projection.", content = @Content(
					mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProjectionModel.class)
			)
	)
	public Response cancelProjection(
			@Parameter(description = "Projection identifier.", required = true) @PathParam(
				"projectionGUID"
			) UUID projectionGUID,
			@RequestBody(
					description = "Optional administrative cancellation detail.", content = @Content(
							mediaType = MediaType.APPLICATION_JSON, schema = @Schema(
									implementation = CancelProjectionRequest.class
							)
					)
			) CancelProjectionRequest request
	) throws ProjectionServiceException {
		String adminCancelReason = request != null ? request.adminCancelReason() : null;
		var started = projectionService.cancelBatchProjection(currentUser.getUser(), projectionGUID, adminCancelReason);
		return Response.status(Status.OK).entity(started).build();
	}

	@POST
	@RolesAllowed({ "USER", "ADMIN" })
	@Path("/{projectionGUID}/duplicate")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "duplicateProjection", summary = "Duplicate a projection", description = "Creates a new projection by copying the configuration and input files of an existing projection."
	)
	@APIResponse(
			responseCode = "200", description = "The duplicated projection.", content = @Content(
					mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProjectionModel.class)
			)
	)
	public Response duplicateProjection(
			@Parameter(description = "Projection identifier.", required = true) @PathParam(
				"projectionGUID"
			) UUID projectionGUID
	) throws ProjectionServiceException {
		var started = projectionService.duplicateProjection(projectionGUID, currentUser.getUser());
		return Response.status(Status.OK).entity(started).build();
	}

	@GET
	@RolesAllowed({ "USER", "ADMIN", "SYSTEM" })
	@Path("/{projectionGUID}/fileset/{fileSetGUID}")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "getProjectionFileSetFiles", summary = "List files in a projection file set", description = "Returns metadata for every file attached to a projection file set."
	)
	@APIResponse(
			responseCode = "200", description = "File metadata for the file set.", content = @Content(
					mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FileMappingModel[].class)
			)
	)
	public Response getFileSetFiles(
			@Parameter(description = "Projection identifier.", required = true) @PathParam(
				"projectionGUID"
			) UUID projectionGUID, //
			@Parameter(description = "Projection file-set identifier.", required = true) @PathParam(
				"fileSetGUID"
			) UUID fileSetGUID
	) throws ProjectionServiceException {
		var found = projectionService.getAllFileSetFiles(projectionGUID, fileSetGUID, currentUser.getUser());
		return Response.status(Status.OK).entity(found).build();
	}

	@GET
	@RolesAllowed({ "USER", "ADMIN" })
	@Path("/{projectionGUID}/resultZip")
	@Produces("application/zip")
	@Operation(
			operationId = "downloadProjectionResults", summary = "Download projection results", description = "Streams the projection result ZIP archive from object storage."
	)
	@APIResponse(
			responseCode = "200", description = "Projection result ZIP archive.", content = @Content(
					mediaType = "application/zip", schema = @Schema(type = SchemaType.STRING, format = "binary")
			)
	)
	@APIResponse(responseCode = "404", description = "The projection has no result files.")
	public Response streamResultsZip(
			@Parameter(description = "Projection identifier.", required = true) @PathParam(
				"projectionGUID"
			) UUID projectionGUID, @Context HttpHeaders headers
	) throws ProjectionServiceException {
		var projection = projectionService.getProjectionEntity(projectionGUID);
		List<FileMappingModel> resultFiles = projectionService.getAllFileSetFiles(
				projectionGUID, projection.getResultFileSet().getProjectionFileSetGUID(), currentUser.getUser()
		);
		if (resultFiles.isEmpty()) {
			throw new WebApplicationException(
					Response.status(Status.NOT_FOUND).entity("No result files found for projection " + projectionGUID)
							.type(MediaType.TEXT_PLAIN).build()
			);
		}
		FileMappingModel file = projectionService.getFileForDownload(
				projectionGUID, projection.getResultFileSet().getProjectionFileSetGUID(),
				UUID.fromString(resultFiles.get(0).getFileMappingGUID()), currentUser.getUser()
		);

		URL upstreamUrl = file.getDownloadURL();

		StreamingOutput stream = (OutputStream out) -> {
			Response upstream = client.target(upstreamUrl.toString()).request("application/zip").get();

			try (upstream) {
				if (upstream.getStatus() != 200) {
					// Propagate error cleanly (don’t stream partial garbage)
					throw new WebApplicationException(
							Response.status(upstream.getStatus()).entity(upstream.readEntity(String.class))
									.type(MediaType.TEXT_PLAIN).build()
					);
				}

				try (InputStream in = upstream.readEntity(InputStream.class)) {
					in.transferTo(out); // Java 9+
					out.flush();
				}
			}
		};

		return Response.ok(stream).header(
				HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"vdyp_output_" + projectionGUID + ".zip\""
		).build();
	}

	@POST
	@RolesAllowed("SYSTEM")
	@Path("/{projectionGUID}/fileset/{fileSetGUID}/file/start")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "startProjectionFileUpload", summary = "Start a direct file upload", description = "Creates an object-storage placeholder and database record before a system caller uploads file content directly."
	)
	@APIResponse(
			responseCode = "201", description = "The pending file mapping and direct-upload location.", content = @Content(
					mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FileMappingModel.class)
			)
	)
	public Response startFileSetFileUpload(
			@Parameter(description = "Projection identifier.", required = true) @PathParam(
				"projectionGUID"
			) UUID projectionGUID,
			@Parameter(description = "Projection file-set identifier.", required = true) @PathParam(
				"fileSetGUID"
			) UUID fileSetGUID,
			@Parameter(
					description = "Name of the file being uploaded.", required = true, example = "results.zip"
			) @QueryParam("filename") String filename
	) throws ProjectionServiceException {
		var created = projectionService.startFileSetFileUpload(projectionGUID, fileSetGUID, filename);
		return Response.status(Status.CREATED).entity(created).build();
	}

	@POST
	@RolesAllowed("SYSTEM")
	@Path("/{projectionGUID}/fileset/{fileSetGUID}/file/{fileMappingGUID}/complete")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "completeProjectionFileUpload", summary = "Complete a direct file upload", description = "Confirms that a system caller completed a direct object-storage upload and returns the final file mapping."
	)
	@APIResponse(
			responseCode = "200", description = "The completed file mapping.", content = @Content(
					mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FileMappingModel.class)
			)
	)
	public Response completeFileSetFileUpload(
			@Parameter(description = "Projection identifier.", required = true) @PathParam(
				"projectionGUID"
			) UUID projectionGUID,
			@Parameter(description = "Projection file-set identifier.", required = true) @PathParam(
				"fileSetGUID"
			) UUID fileSetGUID,
			@Parameter(description = "File mapping identifier.", required = true) @PathParam(
				"fileMappingGUID"
			) UUID fileMappingGUID
	) throws ProjectionServiceException {
		var result = projectionService.completeFileSetFileUpload(projectionGUID, fileSetGUID, fileMappingGUID);
		return Response.status(Status.OK).entity(result).build();
	}

	@POST
	@RolesAllowed({ "USER", "ADMIN", "SYSTEM" })
	@Path("/{projectionGUID}/fileset/{fileSetGUID}/file")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "uploadProjectionFile", summary = "Upload a projection file", description = "Stores a multipart file in object storage and attaches it to a projection file set."
	)
	@RequestBody(
			required = true, description = "File content to upload.", content = @Content(
					mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(
							implementation = ProjectionFileUploadRequest.class
					)
			)
	)
	@APIResponse(
			responseCode = "200", description = "The projection with the newly attached file.", content = @Content(
					mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProjectionModel.class)
			)
	)
	public Response addProjectionFile(
			@Parameter(description = "Projection identifier.", required = true) @PathParam(
				"projectionGUID"
			) UUID projectionGUID, //
			@Parameter(description = "Projection file-set identifier.", required = true) @PathParam(
				"fileSetGUID"
			) UUID fileSetGUID, //
			@RestForm("file") FileUpload file //
	) throws ProjectionServiceException {
		var created = projectionService.addProjectionFile(projectionGUID, fileSetGUID, file, currentUser.getUser());
		return Response.status(Status.OK).entity(created).build();
	}

	@GET
	@RolesAllowed({ "USER", "ADMIN" })
	@Path("/{projectionGUID}/fileset/{fileSetGUID}/file/{fileMappingGUID}")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "getProjectionFileDownload", summary = "Get a projection file download", description = "Returns file metadata containing a temporary object-storage download URL."
	)
	@APIResponse(
			responseCode = "200", description = "File metadata and download URL.", content = @Content(
					mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FileMappingModel.class)
			)
	)
	public Response downloadProjectionFile(
			@Parameter(description = "Projection identifier.", required = true) @PathParam(
				"projectionGUID"
			) UUID projectionGUID, //
			@Parameter(description = "Projection file-set identifier.", required = true) @PathParam(
				"fileSetGUID"
			) UUID fileSetGUID, //
			@Parameter(description = "File mapping identifier.", required = true) @PathParam(
				"fileMappingGUID"
			) UUID fileMappingGUID
	) throws ProjectionServiceException {
		var found = projectionService
				.getFileForDownload(projectionGUID, fileSetGUID, fileMappingGUID, currentUser.getUser());
		return Response.status(Status.OK).entity(found).build();
	}

	@DELETE
	@RolesAllowed({ "USER", "ADMIN" })
	@Path("/{projectionGUID}/fileset/{fileSetGUID}/file/{fileMappingGUID}")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			operationId = "deleteProjectionFile", summary = "Delete a projection file", description = "Deletes an attached file from a projection file set and object storage."
	)
	@APIResponse(responseCode = "200", description = "The file was deleted.")
	public Response deleteProjectionFile(
			@Parameter(description = "Projection identifier.", required = true) @PathParam(
				"projectionGUID"
			) UUID projectionGUID, //
			@Parameter(description = "Projection file-set identifier.", required = true) @PathParam(
				"fileSetGUID"
			) UUID fileSetGUID, //
			@Parameter(description = "File mapping identifier.", required = true) @PathParam(
				"fileMappingGUID"
			) UUID fileMappingGUID
	) throws ProjectionServiceException {
		projectionService.deleteFile(projectionGUID, fileSetGUID, fileMappingGUID, currentUser.getUser());
		return Response.status(Status.OK).build();
	}

}
