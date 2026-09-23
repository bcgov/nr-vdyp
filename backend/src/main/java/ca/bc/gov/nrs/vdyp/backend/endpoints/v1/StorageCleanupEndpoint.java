package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import ca.bc.gov.nrs.vdyp.backend.context.CurrentVDYPUser;
import ca.bc.gov.nrs.vdyp.backend.data.models.StorageCleanupReportModel;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.impl.Endpoint;
import ca.bc.gov.nrs.vdyp.backend.services.StorageCleanupService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v8/admin/storage-cleanup")
@ApplicationScoped
@RolesAllowed("ADMIN")
@Produces(MediaType.APPLICATION_JSON)
public class StorageCleanupEndpoint implements Endpoint {

	private final StorageCleanupService service;
	private final CurrentVDYPUser currentUser;

	public StorageCleanupEndpoint(StorageCleanupService service, CurrentVDYPUser currentUser) {
		this.service = service;
		this.currentUser = currentUser;
	}

	@POST
	@Operation(
			operationId = "cleanupPvcStorage", summary = "Scan (and optionally delete) leftover batch PVC job folders", description = "Defaults to preview. With dryRun=false, permanently deletes job folders that are not Running, Stuck, or currently active in the batch service. Requires the ADMIN role."
	)
	public StorageCleanupReportModel cleanup(
			@Parameter(
					description = "When true (the default), evaluates candidates without deleting anything."
			) @QueryParam("dryRun") @DefaultValue("true") boolean dryRun
	) {
		return service.cleanup(currentUser.getUser(), dryRun);
	}
}
