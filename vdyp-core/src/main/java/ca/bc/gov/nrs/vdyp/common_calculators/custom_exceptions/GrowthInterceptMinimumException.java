package ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions;
/**
 * The GrowthInterceptMinimumException exist to replace the SI_ERR_GI_MIN(-2) error code found in the orginal C code
 * This exception is generally thrown when the growth intercept is below a threshold, this could manifest in something like
 *  breast height age is less than 0.5
 */
public class GrowthInterceptMinimumException extends RuntimeException {
    public GrowthInterceptMinimumException(){}

    public GrowthInterceptMinimumException(String message){
        super(message);
    }

    public GrowthInterceptMinimumException(Throwable cause){
        super(cause);
    }

    public GrowthInterceptMinimumException(String message, Throwable cause){
        super(message, cause);
    }
}
