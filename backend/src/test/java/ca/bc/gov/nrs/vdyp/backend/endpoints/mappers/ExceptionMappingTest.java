package ca.bc.gov.nrs.vdyp.backend.endpoints.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers.ApiError;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers.ProjectionNotFoundExceptionMapper;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers.ProjectionStateExceptionMapper;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers.ProjectionUnauthorizedExceptionMapper;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers.UnhandledExceptionMapper;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionNotFoundException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionServiceException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionStateException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionUnauthorizedException;
import jakarta.ws.rs.core.Response;

class ExceptionMappingTest {

	static Stream<Arguments> ServiceExceptionArguments() {
		return Stream.of(
				Arguments.of("Test Exception", UUID.randomUUID(), UUID.randomUUID()), //
				Arguments.of("Test Exception 2", null, null), //
				Arguments.of("Test Exception 3", UUID.randomUUID(), null) //
		);
	}

	@ParameterizedTest
	@MethodSource("ServiceExceptionArguments")
	void serviceExceptionConstruction(String message, UUID projectionGUID, UUID actingUserGuid) {
		ProjectionServiceException exception1;
		ProjectionServiceException exception2;
		if (projectionGUID != null && actingUserGuid != null) {
			exception1 = new ProjectionServiceException(message, projectionGUID, actingUserGuid);
			exception2 = new ProjectionServiceException(
					message, new Exception("because"), projectionGUID, actingUserGuid
			);
		} else if (projectionGUID != null) {
			exception1 = new ProjectionServiceException(message, projectionGUID);
			exception2 = new ProjectionServiceException(message, new Exception("because"), projectionGUID);
		} else {
			exception1 = new ProjectionServiceException(message);
			exception2 = new ProjectionServiceException(message, new Exception("because"));
		}
		assertEquals(projectionGUID, exception1.getProjectionGuid());
		assertEquals(actingUserGuid, exception1.getActingUserGuid());
		assertEquals(projectionGUID, exception2.getProjectionGuid());
		assertEquals(actingUserGuid, exception2.getActingUserGuid());
		assertEquals("because", exception2.getCause().getMessage());
	}

	@Test
	void unauthorizedExceptionTest() {
		UUID projectionId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		// Instantiate exception
		ProjectionUnauthorizedException exception = new ProjectionUnauthorizedException(projectionId, userId);

		// Instantiate mapper
		ProjectionUnauthorizedExceptionMapper mapper = new ProjectionUnauthorizedExceptionMapper();

		// Act
		try (Response response = mapper.toResponse(exception)) {
			// Assert HTTP semantics
			assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
			assertNotNull(response.getEntity());
			assertInstanceOf(ApiError.class, response.getEntity());

		}
	}

	@Test
	void notFoundExceptionTest() {
		UUID projectionId = UUID.randomUUID();
		// Instantiate exception
		ProjectionNotFoundException exception = new ProjectionNotFoundException(projectionId);

		// Instantiate mapper
		ProjectionNotFoundExceptionMapper mapper = new ProjectionNotFoundExceptionMapper();

		// Act
		try (Response response = mapper.toResponse(exception)) {
			// Assert HTTP semantics
			assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
			assertNotNull(response.getEntity());
			assertInstanceOf(ApiError.class, response.getEntity());

		}
	}

	@Test
	void stateExceptionTest() {
		UUID projectionId = UUID.randomUUID();
		// Instantiate exception
		ProjectionStateException exception = new ProjectionStateException(projectionId, "UPDATE", "INPROGRESS");

		// Instantiate mapper
		ProjectionStateExceptionMapper mapper = new ProjectionStateExceptionMapper();

		// Act
		try (Response response = mapper.toResponse(exception)) {
			// Assert HTTP semantics
			assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
			assertNotNull(response.getEntity());
			assertInstanceOf(ApiError.class, response.getEntity());

		}
	}

	@Test
	void otherExceptionTest() {
		UUID projectionId = UUID.randomUUID();
		// Instantiate exception
		ProjectionServiceException exception = new ProjectionServiceException("Unknown Error", projectionId);

		// Instantiate mapper
		UnhandledExceptionMapper mapper = new UnhandledExceptionMapper();

		// Act
		try (Response response = mapper.toResponse(exception)) {
			// Assert HTTP semantics
			assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
			assertNotNull(response.getEntity());
			assertInstanceOf(ApiError.class, response.getEntity());

		}
	}
}
