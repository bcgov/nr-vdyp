package ca.bc.gov.nrs.vdyp.backend.clients;

import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import ca.bc.gov.nrs.vdyp.backend.model.COMSBucket;
import ca.bc.gov.nrs.vdyp.backend.model.COMSCreateBucketRequest;
import ca.bc.gov.nrs.vdyp.backend.model.COMSObject;
import io.quarkus.rest.client.reactive.ClientBasicAuth;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(configKey = "coms")
@Path("/api/v1")
@ClientBasicAuth(username = "${quarkus.rest-client.coms.username}", password = "${quarkus.rest-client.coms.password}")
@Produces(MediaType.APPLICATION_JSON)
@ClientHeaderParam(name = "x-amz-endpoint", value = "${coms.s3access.endpoint}")
@ClientHeaderParam(name = "x-amz-bucket", value = "${coms.s3access.bucket}")
public interface COMSClient {
	@PUT
	@Path("/bucket")
	@Consumes(MediaType.APPLICATION_JSON)
	COMSBucket createBucket(COMSCreateBucketRequest createBucketRequest);

	@GET
	@Path("/bucket")
	Set<COMSBucket> searchForBucket(
			@QueryParam("bucketId") String bucketId, //
			@QueryParam("active") Boolean active, //
			@QueryParam("key") String key, //
			@QueryParam("displayName") String displayName
	);

	@PUT
	@Path("/object")
	COMSObject createObject(
			@QueryParam("bucketId") String bucketId, @QueryParam("tagset") Map<String, String> tagset,
			@HeaderParam("Content-Disposition") String contentDisposition,
			@HeaderParam("Content-Length") long contentLength, @HeaderParam("Content-Type") String contentType,
			byte[] body
	);

	@GET
	@Path("/object/{objectID}")
	COMSObject getObject(@PathParam("objectID") String objectID);

	@PUT
	@Path("/object/{objectID}")
	COMSObject updateObject(@PathParam("objectID") String objectID);

}
