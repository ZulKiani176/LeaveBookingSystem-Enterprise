package uk.ac.staffs.leavebooking.identity.exceptions;

public class IdentityEmailAlreadyExistsException extends RuntimeException {
    public IdentityEmailAlreadyExistsException(String email) {
        super("A Firebase account with email " + email + " already exists");
    }
}
