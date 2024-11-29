package ca.bc.gov.nrs.vdyp.backend.v1.gen.responses;

import ca.bc.gov.nrs.vdyp.backend.v1.gen.api.HelpEndpoint;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.api.ProjectionEndpoint;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

@RegisterForReflection
public record Link(String rel, String href, String method) {

	public static Link getSelfLink(UriInfo uriInfo) {
		String href = uriInfo.getAbsolutePath().toString();
		return new Link("self", href, "GET");
	}

	public static Link getLink(UriInfo uriInfo, String rel, String method, Class<HelpEndpoint> clazz) {
		String href = UriBuilder.fromUri(uriInfo.getBaseUri()).path(clazz).build().toString();
		return new Link(rel, href, method);
	}

	public static Link getLink(
			UriInfo uriInfo, String rel, String method, Class<ProjectionEndpoint> clazz, String methodName,
			Class<?>... parameterTypes
	) {
		String href = UriBuilder.fromUri(uriInfo.getBaseUri()).path(clazz).path(clazz, methodName).build().toString();
		return new Link(rel, href, method);
	}
}
