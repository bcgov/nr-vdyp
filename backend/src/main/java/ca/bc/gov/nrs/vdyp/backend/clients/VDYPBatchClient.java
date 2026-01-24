package ca.bc.gov.nrs.vdyp.backend.clients;

import java.util.UUID;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestForm;

import ca.bc.gov.nrs.vdyp.backend.data.models.BatchJobModel;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(configKey = "vdyp-batch")
@Path("/api/batch")
public interface VDYPBatchClient {
	@POST
	@Path("/startWithGUIDs")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	BatchJobModel startBatchProcessWithGUID(
			@RestForm("projectionGUID") UUID projectionGUID,
			@RestForm("projectionParametersJson") String projectionParametersJson
	);

	@POST
	@Path("/stop/{batchJobGUID}")
	BatchJobModel stopBatchJob(@PathParam("batchJobGUID") UUID batchJobGUID);

	@POST
	@Path("/status/{batchJobGUID}")
	BatchJobModel batchJobStatus(@PathParam("batchJobGUID") UUID batchJobGUID);

}
