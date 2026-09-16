package uk.ac.staffs.leavebooking.leave.domain.exceptions;

public class InvalidLeaveAllowanceException extends RuntimeException {
    public InvalidLeaveAllowanceException(String message) {
        super(message);
    }
}
