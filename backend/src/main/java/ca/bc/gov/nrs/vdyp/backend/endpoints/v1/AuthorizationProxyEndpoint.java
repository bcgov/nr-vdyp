package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import ca.bc.gov.nrs.vdyp.backend.config.OidcConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@ApplicationScoped
@Path("/auth")
public class AuthorizationProxyEndpoint {

	OidcConfig cfg;
	private final WebClient client = WebClient
			.create(io.vertx.core.Vertx.vertx(), new WebClientOptions().setFollowRedirects(false));

	public AuthorizationProxyEndpoint(OidcConfig cfg) {
		this.cfg = cfg;
	}

	@POST
	@Path("/realms/standard/protocol/openid-connect/token")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response token(@Context UriInfo uriInfo, MultivaluedMap<String, String> form) {
		// Inject client auth for Keycloak confidential client
		form.putSingle("client_secret", cfg.clientSecret());

		String tokenUrl = cfg.authServerUrl() + "/protocol/openid-connect/token";

		String body = toFormUrlEncoded(form);

		var resp = client.postAbs(tokenUrl).putHeader("Content-Type", "application/x-www-form-urlencoded")
				.sendBuffer(Buffer.buffer(body)).toCompletionStage().toCompletableFuture().join();

		return Response.status(resp.statusCode()).type(resp.getHeader("Content-Type")).entity(resp.bodyAsString())
				.build();
	}

	@GET
	@Path("/realms/standard/protocol/openid-connect/auth")
	public Response authorize(@Context UriInfo uriInfo) {

		return forwardSimpleRequest("/protocol/openid-connect/auth", uriInfo);
	}

	@GET
	@Path("/realms/standard/.well-known/openid-configuration")
	@Produces(MediaType.APPLICATION_JSON)
	public Response forwardConfig(@Context UriInfo uriInfo) {
		return forwardSimpleRequest("/.well-known/openid-configuration", uriInfo);
	}

	@GET
	@Path("/realms/standard/protocol/openid-connect/token/introspect")
	@Produces(MediaType.APPLICATION_JSON)
	public Response forwardIntrospect(@Context UriInfo uriInfo) {
		return forwardSimpleRequest("/protocol/openid-connect/token/introspect", uriInfo);
	}

	@GET
	@Path("/realms/standard/protocol/openid-connect/userinfo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response forwardUserInfo(@Context UriInfo uriInfo) {
		return forwardSimpleRequest("/protocol/openid-connect/userinfo", uriInfo);
	}

	@GET
	@Path("/realms/standard/protocol/openid-connect/certs")
	@Produces(MediaType.APPLICATION_JSON)
	public Response forwardCerts(@Context UriInfo uriInfo) {
		return forwardSimpleRequest("/protocol/openid-connect/certs", uriInfo);
	}

	@GET
	@Path("/realms/standard/protocol/openid-connect/logout")
	@Produces(MediaType.APPLICATION_JSON)
	public Response forwardLogout(@Context UriInfo uriInfo) {
		return forwardSimpleRequest("/protocol/openid-connect/logout", uriInfo);
	}

	private Response forwardSimpleRequest(String path, UriInfo uriInfo) {
		String qs = uriInfo.getRequestUri().getRawQuery();
		String target = cfg.authServerUrl() + path + (qs != null ? "?" + qs : "");
		return Response.seeOther(URI.create(target)).build();
	}

	@GET
	@Path("/realms/standard/protocol/openid-connect/3p-cookies/{page}")
	public Uni<Response> forward3pCookies(
			@Context UriInfo uriInfo, @Context jakarta.ws.rs.core.HttpHeaders headers,
			@jakarta.ws.rs.PathParam("page") String page
	) {

		if (!"step1.html".equals(page) && !"step2.html".equals(page)) {
			return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build());
		}

		String qs = uriInfo.getRequestUri().getRawQuery();
		String target = cfg.authServerUrl() + "/protocol/openid-connect/3p-cookies/" + page
				+ (qs != null ? "?" + qs : "");

		return Uni.createFrom().completionStage(
				client.getAbs(target) //
						.putHeader("Accept", headerOr(headers, "Accept", "*/*")) //
						.putHeader("User-Agent", headerOr(headers, "User-Agent", "")) //
						.putHeader("Accept-Language", headerOr(headers, "Accept-Language", "")) //
						.putHeader("Referer", headerOr(headers, "Referer", "")) //
						.send() //
						.toCompletionStage() //
		).onItem().transform(this::buildProxiedResponse);
	}

	@GET
	@Path("/realms/standard/protocol/openid-connect/login-status-iframe.html")
	public Uni<Response>
			forwardLoginStatusIframe(@Context UriInfo uriInfo, @Context jakarta.ws.rs.core.HttpHeaders headers) {

		String qs = uriInfo.getRequestUri().getRawQuery();
		String target = cfg.authServerUrl() + "/protocol/openid-connect/login-status-iframe.html";

		return Uni.createFrom().completionStage(
				client.getAbs(target) //
						.putHeader("Accept", headerOr(headers, "Accept", "*/*")) //
						.putHeader("User-Agent", headerOr(headers, "User-Agent", "")) //
						.putHeader("Accept-Language", headerOr(headers, "Accept-Language", "")) //
						.putHeader("Referer", headerOr(headers, "Referer", "")) //
						.send() //
						.toCompletionStage() //
		).onItem().transform(this::buildProxiedResponse);
	}

	@GET
	@Path("/resources/{path:.*}")
	public Uni<Response> forwardResources(
			@Context UriInfo uriInfo, @Context jakarta.ws.rs.core.HttpHeaders headers,
			@jakarta.ws.rs.PathParam("path") String path
	) {
		// Forward static resource requests to Keycloak
		String target = cfg.authServerUrl().replace("/realms/standard", "") + "/resources/" + path;

		return Uni.createFrom().completionStage(
				client.getAbs(target) //
						.putHeader("Accept", headerOr(headers, "Accept", "*/*")) //
						.putHeader("User-Agent", headerOr(headers, "User-Agent", "")) //
						.putHeader("Accept-Language", headerOr(headers, "Accept-Language", "")) //
						.putHeader("Referer", headerOr(headers, "Referer", "")) //
						.send() //
						.toCompletionStage() //
		).onItem().transform(this::buildProxiedResponse);
	}

	// -------- helpers --------
	private static String toFormUrlEncoded(MultivaluedMap<String, String> form) {
		var sb = new StringBuilder();
		boolean first = true;
		for (var e : form.entrySet()) {
			for (var v : e.getValue()) {
				if (!first)
					sb.append('&');
				first = false;
				sb.append(urlEnc(e.getKey())).append('=').append(urlEnc(v));
			}
		}
		return sb.toString();
	}

	private static String urlEnc(String s) {
		return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

	private Response buildProxiedResponse(HttpResponse<Buffer> resp) {
		Response.ResponseBuilder rb = Response.status(resp.statusCode());

		String contentType = resp.getHeader("Content-Type");
		if (contentType != null)
			rb.type(contentType);

		copyIfPresent(resp, rb, "Cache-Control");
		copyIfPresent(resp, rb, "Pragma");
		copyIfPresent(resp, rb, "Expires");
		resp.headers().getAll("Set-Cookie").forEach(v -> rb.header("Set-Cookie", v));
		copyIfPresent(resp, rb, "Location");

		return rb.entity(resp.bodyAsBuffer() != null ? resp.bodyAsBuffer().getBytes() : new byte[0]).build();
	}

	private static void copyIfPresent(HttpResponse<?> resp, Response.ResponseBuilder rb, String header) {
		String v = resp.getHeader(header);
		if (v != null)
			rb.header(header, v);
	}

	private static String headerOr(HttpHeaders headers, String name, String def) {
		String v = headers.getHeaderString(name);
		return v != null ? v : def;
	}

}
