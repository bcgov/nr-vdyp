package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.impl.Endpoint;
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
public class HelpEndpoint implements Endpoint {

	@Inject
	private HelpService helpService;

	@jakarta.ws.rs.GET
	@Produces({ "application/json" })
	@Tag(
			name = "Get Help", description = "returns a detailed description of the parameters available when executing a projection."
	)
	public Response helpGet(@Context UriInfo uriInfo /* , @Context SecurityContext securityContext */) {
		return Response.ok(helpService.helpGet(uriInfo, null /* securityContext */)).build();
	}
}
