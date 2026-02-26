package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.backend.context.CurrentVDYPUser;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.impl.Endpoint;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionServiceException;
import ca.bc.gov.nrs.vdyp.backend.model.ModelParameters;
import ca.bc.gov.nrs.vdyp.backend.model.ProjectionProgressUpdate;
import ca.bc.gov.nrs.vdyp.backend.services.ProjectionService;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.security.Authenticated;
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
@Tag(name = "Projection API", description = "the projection API")
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

		var polygonFile = polygonDataStream.uploadedFile().toFile();
		var layerFile = layersDataStream.uploadedFile().toFile();

		try {
			try (
					var polyStream = new FileInputStream(polygonFile); //
					var layersStream = new FileInputStream(layerFile)
			) {
				return projectionService.projectionHcsvPost(
						trialRun, parameters, polyStream, layersStream, null /* securityContext */
				);
			} catch (ProjectionRequestValidationException e) {
				return Response.status(Status.BAD_REQUEST).header("content-type", "application/json")
						.entity(serialize(new ValidationMessageListResource(e.getValidationMessages()))).build();
			}
		} catch (Exception e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(e.getMessage() == null ? "unknown reason" : e.getMessage()).build();
		}
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
	@Authenticated
	@Path("/me")
	@Produces({ MediaType.APPLICATION_JSON })
	@Tag(name = "Get Projection List", description = "Get all projections for the current user.")
	public Response getAuthenticatedUserProjections() {
		var projections = projectionService.getAllProjectionsForUser(currentUser.getUserId());
		return Response.ok(projections).status(Response.Status.OK).build();
	}

	@POST
	@Authenticated
	@Path("/new")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({ MediaType.APPLICATION_JSON })
	@Tag(
			name = "Create Default Projection", description = "Create a new empty projection with a full set of parameters set to defaults."
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
	@Authenticated
	@Path("/{projectionGUID}")
	@Produces({ MediaType.APPLICATION_JSON })
	@Tag(name = "Get Projection Details", description = "Get all the details of an existing projection")
	public Response getProjection(@PathParam("projectionGUID") UUID projectionGUID) throws ProjectionServiceException {
		var fetched = projectionService.getProjectionByID(projectionGUID, currentUser.getUser());
		return Response.status(Status.OK).entity(fetched).build();
	}

	@PUT
	@Authenticated
	@Path("/{projectionGUID}/params")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({ MediaType.APPLICATION_JSON })
	@Tag(
			name = "Edit Projection Parameters", description = "Update an existing projection with a full set of parameters. Parameters will not be merged pass the full set every time"
	)
	public Response editProjectionParameters(
			@PathParam("projectionGUID") UUID projectionGUID,
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
	@Authenticated
	@Path("/{projectionGUID}")
	@Produces({ MediaType.APPLICATION_JSON })
	@Tag(name = "Delete Projection", description = "Delete an existing projection.")
	public Response deleteProjection(@PathParam("projectionGUID") UUID projectionGUID)
			throws ProjectionServiceException {
		projectionService.deleteProjection(projectionGUID, currentUser.getUser());
		return Response.status(Status.NO_CONTENT).build();
	}

	@POST
	@Authenticated
	@Path("/{projectionGUID}/run")
	@Produces({ MediaType.APPLICATION_JSON })
	@Tag(name = "Run a Projection", description = "Send a Projection to the processing engine to be run.")
	public Response runProjection(@PathParam("projectionGUID") UUID projectionGUID) throws ProjectionServiceException {
		var started = projectionService.startBatchProjection(currentUser.getUser(), projectionGUID);
		return Response.status(Status.OK).entity(started).build();
	}

	@PATCH
	@Authenticated
	@Path("/{projectionGUID}/progress")
	@Produces({ MediaType.APPLICATION_JSON })
	@Consumes({ MediaType.APPLICATION_JSON })
	@Tag(
			name = "Update Progress of a running projection", description = "(System Only) Updates the progress of a running projection."
	)
	public Response updateCompleteProjectionProgress(
			@PathParam("projectionGUID") UUID projectionGUID, ProjectionProgressUpdate progressUpdate
	) throws ProjectionServiceException {
		projectionService.updateProgress(currentUser.getUser(), projectionGUID, progressUpdate);
		return Response.status(Status.OK).build();
	}

	@POST
	@Authenticated
	@Path("/{projectionGUID}/complete")
	@Produces({ MediaType.APPLICATION_JSON })
	@Tag(
			name = "Set a Projections Status", description = "(System Only) Updates as a projection status to complete after processing."
	)
	public Response updateCompleteProjectionStatus(
			@PathParam("projectionGUID") UUID projectionGUID, @QueryParam("success") boolean success
	) throws ProjectionServiceException {
		var started = projectionService.updateCompleteStatus(currentUser.getUser(), projectionGUID, success);
		return Response.status(Status.OK).entity(started).build();
	}

	private <T> String serialize(T entity) throws JsonProcessingException {
		return mapper.writeValueAsString(entity);
	}

	@POST
	@Authenticated
	@Path("/{projectionGUID}/cancel")
	@Produces({ MediaType.APPLICATION_JSON })
	@Tag(name = "Cancel A Projection", description = "Cancels a running projection.")
	public Response cancelProjection(@PathParam("projectionGUID") UUID projectionGUID)
			throws ProjectionServiceException {
		var started = projectionService.cancelBatchProjection(currentUser.getUser(), projectionGUID);
		return Response.status(Status.OK).entity(started).build();
	}

	@POST
	@Authenticated
	@Path("/{projectionGUID}/duplicate")
	@Produces({ MediaType.APPLICATION_JSON })
	@Tag(name = "Duplicate A Projection", description = "Duplicates the inputs of an existing projection.")
	public Response duplicateProjection(@PathParam("projectionGUID") UUID projectionGUID)
			throws ProjectionServiceException {
		var started = projectionService.duplicateProjection(projectionGUID, currentUser.getUser());
		return Response.status(Status.OK).entity(started).build();
	}

	@GET
	@Authenticated
	@Path("/{projectionGUID}/fileset/{fileSetGUID}")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_JSON })
	@Tag(
			name = "Get lightweight details of files in a fileset for a projection", description = "Gets a list of all files in a projection file set"
	)
	public Response getFileSetFiles(
			@PathParam("projectionGUID") UUID projectionGUID, //
			@PathParam("fileSetGUID") UUID fileSetGUID
	) throws ProjectionServiceException {
		var found = projectionService.getAllFileSetFiles(projectionGUID, fileSetGUID, currentUser.getUser());
		return Response.status(Status.OK).entity(found).build();
	}

	@GET
	@Authenticated
	@Path("/{projectionGUID}/resultZip")
	@Produces("application/zip")
	@Tag(name = "Results Zip", description = "Stream the results ZIP from s3 storage to prevent CORS issues")
	public Response streamResultsZip(@PathParam("projectionGUID") UUID projectionGUID, @Context HttpHeaders headers)
			throws ProjectionServiceException {
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
	@Authenticated
	@Path("/{projectionGUID}/fileset/{fileSetGUID}/file")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_JSON })
	@Tag(
			name = "Upload a file for a FileSet", description = "Stores a file in COMS and attaches it to the projection file set that is chosen"
	)
	public Response addProjectionFile(
			@PathParam("projectionGUID") UUID projectionGUID, //
			@PathParam("fileSetGUID") UUID fileSetGUID, //
			@RestForm("file") FileUpload file //
	) throws ProjectionServiceException {
		var created = projectionService.addProjectionFile(projectionGUID, fileSetGUID, file, currentUser.getUser());
		return Response.status(Status.OK).entity(created).build();
	}

	@GET
	@Authenticated
	@Path("/{projectionGUID}/fileset/{fileSetGUID}/file/{fileMappingGUID}")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_JSON })
	@Tag(name = "Get a file for download", description = "Gets the mapped file with a presigned url for download")
	public Response downloadProjectionFile(
			@PathParam("projectionGUID") UUID projectionGUID, //
			@PathParam("fileSetGUID") UUID fileSetGUID, //
			@PathParam("fileMappingGUID") UUID fileMappingGUID
	) throws ProjectionServiceException {
		var found = projectionService
				.getFileForDownload(projectionGUID, fileSetGUID, fileMappingGUID, currentUser.getUser());
		return Response.status(Status.OK).entity(found).build();
	}

	@DELETE
	@Authenticated
	@Path("/{projectionGUID}/fileset/{fileSetGUID}/file/{fileMappingGUID}")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_JSON })
	@Tag(
			name = "Delete a file from a fileset for a projection", description = "Deletes a file from a projection file sets"
	)
	public Response deleteProjectionFile(
			@PathParam("projectionGUID") UUID projectionGUID, //
			@PathParam("fileSetGUID") UUID fileSetGUID, //
			@PathParam("fileMappingGUID") UUID fileMappingGUID
	) throws ProjectionServiceException {
		projectionService.deleteFile(projectionGUID, fileSetGUID, fileMappingGUID, currentUser.getUser());
		return Response.status(Status.OK).build();
	}

}
