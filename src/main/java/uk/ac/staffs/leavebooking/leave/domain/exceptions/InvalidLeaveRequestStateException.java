package uk.ac.staffs.leavebooking.leave.domain.exceptions;

public class InvalidLeaveRequestStateException extends RuntimeException {
    public InvalidLeaveRequestStateException(String message) {
        super(message);
    }
}
