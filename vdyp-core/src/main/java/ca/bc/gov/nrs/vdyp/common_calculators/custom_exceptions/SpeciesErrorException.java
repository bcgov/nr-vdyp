package ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions;

/**
 * The SpeciesErrorException exist to replace the SI_ERR_SPEC(-10) error code
 * found in the orginal C code This exception is generally thrown when if the
 * input parameter is not a valid species index or there is another species
 * related error
 */
public class SpeciesErrorException extends RuntimeException {
	public SpeciesErrorException() {
	}

	public SpeciesErrorException(String message) {
		super(message);
	}

	public SpeciesErrorException(Throwable cause) {
		super(cause);
	}

	public SpeciesErrorException(String message, Throwable cause) {
		super(message, cause);
	}
}
