package ca.bc.gov.nrs.vdyp.backend.v1.gen.api;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import ca.bc.gov.nrs.vdyp.backend.v1.api.RootService;
import ca.bc.gov.nrs.vdyp.backend.v1.api.impl.exceptions.NotFoundException;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.api.impl.Endpoint;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/api/v8")
@RegisterForReflection
public class RootEndpoint implements Endpoint {

	@Inject
	private RootService rootResourceApi;

	public RootEndpoint() {
	}

	@jakarta.ws.rs.GET

	@Tag(
			name = "Get Root", description = "Returns the top level resource of the service, including links to all other top level resources."
	)
	public Response rootGet(@Context UriInfo uriInfo /* , @Context SecurityContext securityContext */)
			throws NotFoundException {
		return Response.ok(rootResourceApi.rootGet(uriInfo, null)).build();
	}
}
