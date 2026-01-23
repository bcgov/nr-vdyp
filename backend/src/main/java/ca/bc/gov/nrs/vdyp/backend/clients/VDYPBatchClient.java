package ca.bc.gov.nrs.vdyp.backend.clients;

import java.util.UUID;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import ca.bc.gov.nrs.vdyp.backend.data.models.BatchJobModel;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(configKey = "vdyp-batch")
@Path("/api/batch")
@Produces(MediaType.APPLICATION_JSON)
public interface VDYPBatchClient {
	@POST
	@Path("/startWithGUIDs")
	BatchJobModel startBatchProcessWithGUID(
			@QueryParam("projectionGUID") UUID projectionGUID,
			@QueryParam("projectionParametersJson") String projectionParametersJson
	);

	@POST
	@Path("/stop/{batchJobGUID}")
	BatchJobModel stopBatchJob(@PathParam("batchJobGUID") UUID batchJobGUID);

	@POST
	@Path("/status/{batchJobGUID}")
	BatchJobModel batchJobStatus(@PathParam("batchJobGUID") UUID batchJobGUID);

}
