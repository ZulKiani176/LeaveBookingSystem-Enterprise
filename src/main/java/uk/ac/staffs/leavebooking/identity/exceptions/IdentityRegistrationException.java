package uk.ac.staffs.leavebooking.identity.exceptions;

public class IdentityRegistrationException extends RuntimeException {
    public IdentityRegistrationException(String message) {
        super(message);
    }

    public IdentityRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
