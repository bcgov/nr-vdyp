package ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions;
/**
 * The GrowthInterceptTotalException exist to replace the SI_ERR_GI_TOT(-9) error code found in the orginal C code
 * This exception is generally thrown when there is an issue growth intercept(GI) curve or total age
 */
public class GrowthInterceptTotalException extends RuntimeException {
    public GrowthInterceptTotalException(){}

    public GrowthInterceptTotalException(String message){
        super(message);
    }

    public GrowthInterceptTotalException(Throwable cause){
        super(cause);
    }

    public GrowthInterceptTotalException(String message, Throwable cause){
        super(message, cause);
    }
}
