package ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public abstract class AbstractApiExceptionMapper<E extends Throwable> implements ExceptionMapper<E> {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public final Response toResponse(E exception) {
		log(exception);
		return buildResponse(exception);
	}

	/**
	 * Override if you want different log levels or formats.
	 */
	protected void log(E exception) {
		logger.error(exception.getMessage(), exception);
	}

	/**
	 * Child classes define HTTP semantics.
	 */
	protected abstract Response buildResponse(E exception);

	/**
	 * Shared helper for consistent error bodies.
	 */
	protected Response response(Response.Status status, String code, String message) {
		return Response.status(status).entity(new ApiError(code, message)).build();
	}
}
