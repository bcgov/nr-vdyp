package ca.bc.gov.nrs.vdyp.common;

public class VdypApplicationException extends Exception {

	private static final long serialVersionUID = 5386505764229461090L;

	public VdypApplicationException(Exception cause) {
		super(cause);
	}
}
