package ca.bc.gov.nrs.vdyp.backend.clients;

import java.util.Map;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import ca.bc.gov.nrs.vdyp.backend.model.COMSCreateObjectResponse;
import io.quarkus.oidc.token.propagation.AccessToken;
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
@AccessToken
@Path("/api/v1/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ClientHeaderParam(name = "x-amz-endpoint", value = "${coms.s3access.endpoint}")
@ClientHeaderParam(name = "x-amz-bucket", value = "${coms.s3access.bucket}")
public interface COMSClient {
	@PUT
	@Path("/bucket")
	COMSCreateBucketResponse createBucket(COMECreateBucketRequest createBucketRequest);

	@GET
	@Path("/bucket")
	COMSSearchBucketResponse searchForBucket(COMSSearchBucketRequest searchBucketRequest);

	@PUT
	@Path("/object")
	COMSCreateObjectResponse createObject(
			@QueryParam("bucketId") String bucketId, @QueryParam("tagset") Map<String, String> tagset,
			@HeaderParam("Content-Disposition") String contentDisposition,
			@HeaderParam("Content-Length") long contentLength, @HeaderParam("Content-Type") String contentType,
			byte[] body
	);

	@GET
	@Path("/object/{objectID}")
	COMSGetObjectResponse getObject(@PathParam("objectID") String objectID);

	@PUT
	@Path("/object/{objectID}")
	COMSGetObjectResponse updateObject(@PathParam("objectID") String objectID);

}
