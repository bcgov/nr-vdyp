package ca.bc.gov.nrs.vdyp.backend.clients;

import java.io.InputStream;
import java.util.List;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import ca.bc.gov.nrs.vdyp.backend.model.COMSBucket;
import ca.bc.gov.nrs.vdyp.backend.model.COMSCreateBucketRequest;
import ca.bc.gov.nrs.vdyp.backend.model.COMSObject;
import io.quarkus.rest.client.reactive.ClientBasicAuth;
import jakarta.json.JsonString;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RegisterRestClient(configKey = "coms")
@Path("/api/v1")
@ClientBasicAuth(username = "${coms.basic-auth.username}", password = "${coms.basic-auth.password}")
@Produces(MediaType.APPLICATION_JSON)
@ClientHeaderParam(name = "x-amz-endpoint", value = "${coms.s3access.endpoint}")
@ClientHeaderParam(name = "x-amz-bucket", value = "${coms.s3access.bucket}")
public interface COMSClient {
	enum FileDownloadMode {
		PROXY("proxy"), URL("url");

		private final String paramValue;

		FileDownloadMode(String paramValue) {
			this.paramValue = paramValue;
		}

		public String getParamValue() {
			return this.paramValue;
		}

	}

	@PUT
	@Path("/bucket")
	@Consumes(MediaType.APPLICATION_JSON)
	COMSBucket createBucket(COMSCreateBucketRequest createBucketRequest);

	@GET
	@Path("/bucket")
	List<COMSBucket> searchForBucket(
			@QueryParam("bucketId") String bucketId, //
			@QueryParam("active") Boolean active, //
			@QueryParam("key") String key, //
			@QueryParam("displayName") String displayName
	);

	@DELETE
	@Path("/bucket/{bucketId}")
	Response deleteBucket(
			@PathParam("bucketId") String bucketId, //
			@QueryParam("recursive") boolean recursive
	);

	@PUT
	@Path("/object")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	COMSObject createObject(
			@QueryParam("bucketId") String bucketId, //
			@HeaderParam("Content-Disposition") String contentDisposition, //
			@HeaderParam("Content-Length") long contentLength, //
			@HeaderParam("Content-Type") String contentType, //
			InputStream body
	);

	@PUT
	@Path("/object/{objectId}")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	COMSObject updateObject(
			@PathParam("objectId") String objectId, //
			@HeaderParam("Content-Disposition") String contentDisposition, //
			@HeaderParam("Content-Length") long contentLength, //
			@HeaderParam("Content-Type") String contentType, //
			InputStream body
	);

	@DELETE
	@Path("/object/{objectId}")
	Response deleteObject(@PathParam("objectId") String objectId);

	@GET
	@Path("/object/{objectId}")
	@Consumes(MediaType.APPLICATION_JSON)
	JsonString getObject(@PathParam("objectId") String objectId, @QueryParam("download") String download);

}
