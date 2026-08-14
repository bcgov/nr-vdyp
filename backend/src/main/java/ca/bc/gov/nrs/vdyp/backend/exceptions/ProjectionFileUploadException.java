package ca.bc.gov.nrs.vdyp.backend.exceptions;

import jakarta.ws.rs.core.Response;

public class ProjectionFileUploadException extends ProjectionServiceException {
	private final Response.Status status;
	private final String code;

	private ProjectionFileUploadException(Response.Status status, String code, String message, Throwable cause) {
		super(message, cause);
		this.status = status;
		this.code = code;
	}

	public static ProjectionFileUploadException payloadTooLarge(String message) {
		return new ProjectionFileUploadException(
				Response.Status.REQUEST_ENTITY_TOO_LARGE, "PAYLOAD_TOO_LARGE", message, null
		);
	}

	public static ProjectionFileUploadException unsupportedMediaType(String message) {
		return new ProjectionFileUploadException(
				Response.Status.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", message, null
		);
	}

	public static ProjectionFileUploadException invalidCsv(String message) {
		return new ProjectionFileUploadException(Response.Status.BAD_REQUEST, "BAD_REQUEST", message, null);
	}

	public static ProjectionFileUploadException invalidCsv(String message, Throwable cause) {
		return new ProjectionFileUploadException(Response.Status.BAD_REQUEST, "BAD_REQUEST", message, cause);
	}

	public Response.Status getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}
}
