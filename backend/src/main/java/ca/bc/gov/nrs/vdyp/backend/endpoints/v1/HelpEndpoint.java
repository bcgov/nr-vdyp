package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.impl.Endpoint;
import ca.bc.gov.nrs.vdyp.backend.responses.v1.HelpResource;
import ca.bc.gov.nrs.vdyp.backend.services.HelpService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/api/v8/help")
@RegisterForReflection
@Tag(name = "Service information", description = "Discover the VDYP API and its top-level resources.")
public class HelpEndpoint implements Endpoint {

	@Inject
	private HelpService helpService;

	@jakarta.ws.rs.GET
	@Produces({ "application/json" })
	@SecurityRequirements
	@Operation(
			operationId = "getProjectionParameterHelp", summary = "Get projection parameter help", description = "Returns detailed descriptions and defaults for the parameters accepted by projection operations."
	)
	@APIResponse(
			responseCode = "200", description = "Projection parameter documentation.", content = @Content(
					mediaType = "application/json", schema = @Schema(implementation = HelpResource.class)
			)
	)
	public Response helpGet(@Context UriInfo uriInfo /* , @Context SecurityContext securityContext */) {
		return Response.ok(helpService.helpGet(uriInfo, null /* securityContext */)).build();
	}
}
