package uk.ac.staffs.leavebooking.leave.application.exceptions;

public class ConcurrentLeaveModificationException extends RuntimeException {
    public static final String MESSAGE =
            "The resource was modified by another request. Please retry.";

    public ConcurrentLeaveModificationException() {
        super(MESSAGE);
    }

    public ConcurrentLeaveModificationException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
