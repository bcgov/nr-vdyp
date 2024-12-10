package ca.bc.gov.nrs.api.v1.endpoints;

import static io.restassured.RestAssured.given;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class HelpEndpointTest {

	@SuppressWarnings("unused")
	private final TestHelper testHelper;

	@Inject
	HelpEndpointTest(TestHelper testHelper) {
		this.testHelper = testHelper;
	}

	@BeforeEach
	void setup() {
	}

	@Test
	void testGetHelp_shouldReturnStatusOK() {

		given().basePath(TestHelper.ROOT_PATH).when().get("/help").then().statusCode(200).and()
				.contentType("application/json").and()
				.body(Matchers.containsString("outputFormat"), Matchers.containsString("Output Data Format"));
	}

//   @Test
//   void testGetUserById_givenValidID_shouldReturnTheUserAndStatusOK() {
//     given()r
//       .basePath("/api/v8")
//       .pathParam("id", userEntity.getId())
//       .when().get("/users/{id}")
//       .then()
//       .statusCode(200)
//       .body("name", equalTo(userEntity.getName()))
//       .body("email", equalTo(userEntity.getEmail()));
//   }

//   @Test
//   void testGetUserById_givenRandomID_shouldReturnTheUserAndStatusOK() {
//     given()
//       .basePath("/api/v8")
//       .pathParam("id", 20000)
//       .when().get("/users/{id}")
//       .then()
//       .statusCode(404);
//   }

//   @Test
//   void testCreateUser_givenValidPayload_shouldReturnStatusCreated() {
//     var name = faker.name().fullName();
//     var email = faker.internet().emailAddress();
//     User user = new User(null, name, email);
//     given()
//       .basePath("/api/v8")
//       .contentType(ContentType.JSON)
//       .body(user)
//       .when().post("/users")
//       .then()
//       .statusCode(201)
//       .body("name", equalTo(name))
//       .body("email", equalTo(email));
//   }

//   @Test
//   void testCreateUser_givenInValidPayload_shouldReturnStatusBadRequest() {
//     var name = faker.name().fullName();
//     var email = faker.internet().domainName();
//     User user = new User(null, name, email);
//     given()
//       .basePath("/api/v8")
//       .contentType(ContentType.JSON)
//       .body(user)
//       .when().post("/users")
//       .then()
//       .statusCode(400);
//   }

//   @Test
//   void testUpdateUser_givenValidPayload_shouldReturnStatusOK() {
//     var name = faker.name().fullName();
//     var email = faker.internet().emailAddress();
//     User user = new User(userEntity.getId(), name, email);
//     given()
//       .basePath("/api/v8")
//       .contentType(ContentType.JSON)
//       .pathParam("id", userEntity.getId())
//       .body(user)
//       .when().put("/users/{id}")
//       .then()
//       .statusCode(200)
//       .body("name", equalTo(user.name()))
//       .body("email", equalTo(user.email()));
//   }

//   @Test
//   void testDeleteUser_givenValidID_shouldReturnStatusNoContent() {
//     given()
//       .basePath("/api/v8")
//       .pathParam("id", userEntity.getId())
//       .when().delete("/users/{id}")
//       .then()
//       .statusCode(204);
//   }

//   @Test
//   void testDeleteUser_givenInvalidID_shouldReturnStatusNotFound() {
//     given()
//       .basePath("/api/v8")
//       .pathParam("id", 100003330)
//       .when().delete("/users/{id}")
//       .then()
//       .statusCode(404);
//   }

//   @Test
//   void testGetUserAddresses_noCondition_shouldReturnAllUsersAddressesAndStatusOK() {
//     given()
//       .basePath("/api/v8")
//       .pathParam("id", userEntity.getId())
//       .when().get("/users/{id}/addresses")
//       .then()
//       .statusCode(200)
//       .body("$.size()", equalTo(1));
//   }

//   @Test
//   void testCreateUserAddress_givenValidPayload_shouldCreateTheUserAddressAndReturnStatusCreated() {
//     var street = faker.address().streetAddress();
//     var city =faker.address().city();
//     var state =faker.address().state();
//     var zipCode= faker.address().zipCode();
//     UserAddress userAddress = new UserAddress(null, street, city, state, zipCode, userEntity.getId());
//     given()
//       .basePath("/api/v8")
//       .contentType(ContentType.JSON)
//       .pathParam("id", userEntity.getId())
//       .body(userAddress)
//       .when().post("/users/{id}/addresses")
//       .then()
//       .statusCode(201)
//       .body("street", equalTo(street))
//       .body("city", equalTo(city))
//       .body("state", equalTo(state))
//       .body("zipCode", equalTo(zipCode));
//   }

//   @Test
//   void testUpdateUserAddress_givenValidPayload_shouldUpdateTheUserAddressAndReturnStatusOK() {
//     var street = faker.address().streetAddress();
//     var city =faker.address().city();
//     var state =faker.address().state();
//     var zipCode= faker.address().zipCode();
//     UserAddress userAddress = new UserAddress(addressEntity.getId(), street, city, state, zipCode, userEntity.getId());
//     given()
//       .basePath("/api/v8")
//       .contentType(ContentType.JSON)
//       .pathParam("id", userEntity.getId())
//       .pathParam("addressId", addressEntity.getId())
//       .body(userAddress)
//       .when().put("/users/{id}/addresses/{addressId}")
//       .then()
//       .statusCode(200)
//       .body("street", equalTo(street))
//       .body("city", equalTo(city))
//       .body("state", equalTo(state))
//       .body("zipCode", equalTo(zipCode));
//   }

//   @Test
//   void testDeleteUserAddress_givenValidPayload_shouldDeleteTheUserAddressAndReturnStatusNoContent() {
//     given()
//       .basePath("/api/v8")
//       .pathParam("id", userEntity.getId())
//       .pathParam("addressId", addressEntity.getId())
//       .when().delete("/users/{id}/addresses/{addressId}")
//       .then()
//       .statusCode(204);
//   }
}
