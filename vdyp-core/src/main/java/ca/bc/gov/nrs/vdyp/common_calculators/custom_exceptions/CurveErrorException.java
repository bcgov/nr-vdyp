package ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions;
/**
 * The CurveErrorException exist to replace the SI_ERR_CURVE(-5) error code found in the orginal C code
 * This exception is generally thrown when the curve index is unknown, not valid or a similar issue
 */
public class CurveErrorException extends RuntimeException {
    public CurveErrorException(){}

    public CurveErrorException(String message){
        super(message);
    }

    public CurveErrorException(Throwable cause){
        super(cause);
    }

    public CurveErrorException(String message, Throwable cause){
        super(message, cause);
    }
}
