package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

public class ProjectionInternalExecutionException extends Exception {

	private static final long serialVersionUID = -3026466812172806593L;

	public ProjectionInternalExecutionException(Exception cause) {
		super(cause);
	}

	public ProjectionInternalExecutionException(String reason) {
		super(reason);
	}

	public ProjectionInternalExecutionException(Exception cause, String reason) {
		super(reason, cause);
	}
}
