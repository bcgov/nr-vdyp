package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

public class ProjectionInternalExecutionException extends AbstractProjectionRequestException {

	private static final long serialVersionUID = -3026466812172806593L;

	public ProjectionInternalExecutionException(Exception cause) {
		super(cause);
	}

	public ProjectionInternalExecutionException(String reason) {
		super(reason);
	}

	public ProjectionInternalExecutionException(String reason, Exception cause) {
		super(reason, cause);
	}
}
