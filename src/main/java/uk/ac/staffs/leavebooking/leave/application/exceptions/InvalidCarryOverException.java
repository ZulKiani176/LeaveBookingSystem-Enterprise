package uk.ac.staffs.leavebooking.leave.application.exceptions;

public class InvalidCarryOverException extends RuntimeException {
    public InvalidCarryOverException(String message) {
        super(message);
    }
}
