package ca.bc.gov.nrs.vdyp.backend.endpoints.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

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
			assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
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
