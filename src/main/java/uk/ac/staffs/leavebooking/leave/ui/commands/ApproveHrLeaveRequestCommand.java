package uk.ac.staffs.leavebooking.leave.ui.commands;

public record ApproveHrLeaveRequestCommand(String leaveRequestId, String comment) {
    public ApproveHrLeaveRequestCommand(String leaveRequestId) {
        this(leaveRequestId, null);
    }
}
