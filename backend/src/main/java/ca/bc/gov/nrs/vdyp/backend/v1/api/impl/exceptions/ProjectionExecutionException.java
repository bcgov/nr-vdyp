package ca.bc.gov.nrs.vdyp.backend.v1.api.impl.exceptions;

public class ProjectionExecutionException extends Exception {

	private static final long serialVersionUID = -3026466812172806593L;

	public ProjectionExecutionException(Exception cause) {
		super(cause);
	}

	public ProjectionExecutionException(String reason) {
		super(reason);
	}

	public ProjectionExecutionException(Exception cause, String reason) {
		super(reason, cause);
	}
}
