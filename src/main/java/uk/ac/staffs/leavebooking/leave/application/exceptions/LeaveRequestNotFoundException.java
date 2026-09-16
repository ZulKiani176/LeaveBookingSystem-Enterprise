package uk.ac.staffs.leavebooking.leave.application.exceptions;

public class LeaveRequestNotFoundException extends RuntimeException {
    public LeaveRequestNotFoundException(String leaveRequestId) {
        super("Leave request not found: " + leaveRequestId);
    }
}
