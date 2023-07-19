package ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions;
/**
 * The EstablishmentErrorException exist to replace the SI_ERR_ESTAB(-12) error code found in the orginal C code
 * This exception is generally thrown when the establisment type is unknown, not valid or a similar issue
 */
public class EstablishmentErrorException extends RuntimeException {
    public EstablishmentErrorException(){}

    public EstablishmentErrorException(String message){
        super(message);
    }

    public EstablishmentErrorException(Throwable cause){
        super(cause);
    }

    public EstablishmentErrorException(String message, Throwable cause){
        super(message, cause);
    }
}
