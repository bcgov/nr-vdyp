package ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions;
/**
 * The ForestInventoryZoneException exist to replace the SI_ERR_FIZ(-7) error code found in the orginal C code
 * This exception is generally thrown when there is an issue with the FIZ code, such as it being unknown
 */
public class ForestInventoryZoneException extends RuntimeException{
    public ForestInventoryZoneException(){}

    public ForestInventoryZoneException(String message){
        super(message);
    }

    public ForestInventoryZoneException(Throwable cause){
        super(cause);
    }

    public ForestInventoryZoneException(String message, Throwable cause){
        super(message, cause);
    }
}
