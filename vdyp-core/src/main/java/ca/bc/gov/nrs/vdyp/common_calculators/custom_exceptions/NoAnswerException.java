package ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions;

/**
 * The NoAnswerException exist to replace the SI_ERR_NO_ANS(-4) error code found in the orginal C code
 * This exception is generally thrown when some sort of compute value is out bounds or not a valid answer
 */
public class NoAnswerException extends RuntimeException{
    public NoAnswerException(){}

    public NoAnswerException(String message){
        super(message);
    }

    public NoAnswerException(Throwable cause){
        super(cause);
    }

    public NoAnswerException(String message, Throwable cause){
        super(message, cause);
    }
}
