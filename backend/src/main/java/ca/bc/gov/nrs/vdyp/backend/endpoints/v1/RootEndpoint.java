package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.impl.Endpoint;
import ca.bc.gov.nrs.vdyp.backend.responses.v1.RootResource;
import ca.bc.gov.nrs.vdyp.backend.services.RootService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/api/v8")
@RegisterForReflection
@Tag(name = "Service information", description = "Discover the VDYP API and its top-level resources.")
public class RootEndpoint implements Endpoint {

	@Inject
	private RootService rootResourceApi;

	public RootEndpoint() {
	}

	@jakarta.ws.rs.GET
	@Operation(
			operationId = "getApiRoot", summary = "Get the API root", description = "Returns the top-level service resource and links to the other top-level resources."
	)
	@APIResponse(
			responseCode = "200", description = "The API root resource.", content = @Content(
					mediaType = "application/json", schema = @Schema(implementation = RootResource.class)
			)
	)
	public Response rootGet(@Context UriInfo uriInfo /* , @Context SecurityContext securityContext */) {
		return Response.ok(rootResourceApi.rootGet(uriInfo, null)).build();
	}
}
