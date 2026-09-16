package uk.ac.staffs.leavebooking.leave.application.exceptions;

public class OverlappingLeaveRequestException extends RuntimeException {
    public OverlappingLeaveRequestException() {
        super("The requested absence overlaps an active leave request");
    }
}
