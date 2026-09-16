package uk.ac.staffs.leavebooking.leave.domain.exceptions;

public class InvalidLeaveRequestException extends RuntimeException {
    public InvalidLeaveRequestException(String message) {
        super(message);
    }
}
