package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
class OpenApiDocumentTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	@TestSecurity(user = "openapi-test-user", roles = { "USER" })
	void generatedDocumentDescribesThePublicBackendApi() throws Exception {
		String document = given().queryParam("format", "json").accept(MediaType.APPLICATION_JSON).when()
				.get("/q/openapi").then().statusCode(200).extract().asString();
		JsonNode openApi = MAPPER.readTree(document);

		assertEquals("Variable Density Yield Projection API", openApi.at("/info/title").asText());
		assertEquals("http", openApi.at("/components/securitySchemes/bearerAuth/type").asText());
		assertEquals("bearer", openApi.at("/components/securitySchemes/bearerAuth/scheme").asText());
		assertTrue(openApi.at("/security/0/bearerAuth").isArray());

		JsonNode paths = openApi.path("paths");
		assertTrue(paths.has("/api/v8"));
		assertTrue(paths.has("/api/v8/help"));
		assertTrue(paths.has("/api/v8/projection/hcsv"));
		assertTrue(paths.has("/api/v8/projection/{projectionGUID}/fileset/{fileSetGUID}/file"));
		assertFalse(paths.has("/auth/realms/standard/protocol/openid-connect/token"));
		assertFalse(paths.has("/api/batch/capacity"));
		assertFalse(paths.has("/api/v1/bucket"));

		JsonNode hcsv = openApi.at("/paths/~1api~1v8~1projection~1hcsv/post");
		assertEquals("runHcsvProjection", hcsv.path("operationId").asText());
		assertEquals(
				"#/components/schemas/HcsvProjectionRequest",
				hcsv.at("/requestBody/content/multipart~1form-data/schema/$ref").asText()
		);
		assertEquals("binary", hcsv.at("/responses/200/content/application~1octet-stream/schema/format").asText());

		JsonNode hcsvRequest = openApi.at("/components/schemas/HcsvProjectionRequest");
		assertTrue(hcsvRequest.path("required").toString().contains("HCSV-Polygon"));
		assertTrue(hcsvRequest.path("required").toString().contains("HCSV-Layers"));
		assertEquals("binary", hcsvRequest.at("/properties/HCSV-Polygon/format").asText());
		assertEquals("binary", hcsvRequest.at("/properties/HCSV-Layers/format").asText());
		assertTrue(openApi.at("/components/schemas/Parameters/properties").size() > 10);

		JsonNode projectionList = openApi
				.at("/paths/~1api~1v8~1projection~1me/get/responses/200/content/application~1json/schema");
		assertEquals("array", projectionList.path("type").asText());
		assertEquals("#/components/schemas/ProjectionModel", projectionList.at("/items/$ref").asText());

		assertTrue(openApi.at("/paths/~1api~1v8~1help/get/security").isArray());
		assertTrue(openApi.at("/paths/~1api~1v8~1help/get/security").isEmpty());
		assertTrue(openApi.at("/paths/~1api~1v8~1projection~1scsv/post/security").isArray());
		assertTrue(openApi.at("/paths/~1api~1v8~1projection~1scsv/post/security").isEmpty());
	}
}
