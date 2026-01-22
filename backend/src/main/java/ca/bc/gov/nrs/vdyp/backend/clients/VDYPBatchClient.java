package ca.bc.gov.nrs.vdyp.backend.clients;

import java.util.UUID;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import ca.bc.gov.nrs.vdyp.backend.model.BatchProcessingModel;
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
	@Path("/startWithGUID")
	BatchProcessingModel startBatchProcessWithGUID(@QueryParam("projectionGUID") UUID projectionGUID);

	@POST
	@Path("/stop/{batchJobGUID}")
	BatchProcessingModel stopBatchJob(@PathParam("batchJobGUID") UUID batchJobGUID);

	@POST
	@Path("/status/{batchJobGUID}")
	BatchProcessingModel batchJobStatus(@PathParam("batchJobGUID") UUID batchJobGUID);

}
