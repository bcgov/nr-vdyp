package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import java.io.FileInputStream;
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
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.impl.Endpoint;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionServiceException;
import ca.bc.gov.nrs.vdyp.backend.model.FileMetadata;
import ca.bc.gov.nrs.vdyp.backend.services.ProjectionService;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/api/v8/projection")
@Tag(name = "Projection API", description = "the projection API")
@RegisterForReflection
public class ProjectionEndpoint implements Endpoint {

	private static final Logger logger = LoggerFactory.getLogger(ProjectionEndpoint.class);
	private final ProjectionService projectionService;

	private final CurrentVDYPUser currentUser;

	public ProjectionEndpoint(ProjectionService service, CurrentVDYPUser currentUser) {
		this.projectionService = service;
		this.currentUser = currentUser;
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
						.entity(
								serialize(
										ValidationMessageListResource.class,
										new ValidationMessageListResource(e.getValidationMessages())
								)
						).build();
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
			) Parameters parameters
	) throws ProjectionServiceException {
		var created = projectionService.createNewProjection(currentUser.getUser(), parameters);
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
			) Parameters parameters
	) throws ProjectionServiceException {
		var created = projectionService.editProjectionParameters(projectionGUID, currentUser.getUser(), parameters);
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

	private static ObjectMapper mapper = new ObjectMapper();

	private <T> String serialize(Class<T> clazz, T entity) throws JsonProcessingException {
		return mapper.writeValueAsString(entity);
	}

	@PUT
	@Authenticated
	@Path("/{projectionGUID}/fileset/{fileSetGUID}/getuploadurl")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_JSON })
	@Tag(
			name = "Set the url for uploading url", description = "Fetches the presigned url for uploading a file to a fileset for this projection"
	)
	public Response addProjectionFile(
			@PathParam("projectionGUID") UUID projectionGUID, @PathParam("fileSetGUID") UUID fileSetGUID,
			@RestForm(value = "metadata") @PartType(MediaType.APPLICATION_JSON) FileMetadata metadata
	) throws ProjectionServiceException {
		var created = projectionService.addProjectionFile(projectionGUID, fileSetGUID, currentUser.getUser(), metadata);
		return Response.status(Status.OK).entity(created).build();
	}

}
