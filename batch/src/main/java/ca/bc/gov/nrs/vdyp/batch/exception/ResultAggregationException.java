package ca.bc.gov.nrs.vdyp.batch.exception;

public class ResultAggregationException extends RuntimeException {

	public ResultAggregationException(String message) {
		super(message);
	}

	public ResultAggregationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResultAggregationException(Throwable cause) {
		super(cause);
	}
}