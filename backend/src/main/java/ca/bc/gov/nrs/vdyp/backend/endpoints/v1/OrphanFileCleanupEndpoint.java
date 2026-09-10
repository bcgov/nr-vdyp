package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import org.eclipse.microprofile.openapi.annotations.Operation;

import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.impl.Endpoint;
import ca.bc.gov.nrs.vdyp.backend.services.OrphanFileCleanupService;
import ca.bc.gov.nrs.vdyp.backend.services.OrphanFileCleanupService.CleanupReport;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v8/admin/orphan-file-sets")
@ApplicationScoped
@RolesAllowed("ADMIN")
@Produces(MediaType.APPLICATION_JSON)
public class OrphanFileCleanupEndpoint implements Endpoint {

	private final OrphanFileCleanupService service;

	public OrphanFileCleanupEndpoint(OrphanFileCleanupService service) {
		this.service = service;
	}

	@POST
	@Operation(
			operationId = "cleanupOrphanFileSets", summary = "Reconcile S3 file sets after database cutover", description = "Defaults to preview.  With dryRun=false, permanently deletes orphaned S3 versions. Stop all writers before running."
	)
	public CleanupReport cleanup(@QueryParam("dryRun") @DefaultValue("true") boolean dryRun) {
		return service.cleanup(dryRun);
	}
}
