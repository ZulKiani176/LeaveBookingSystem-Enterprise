package uk.ac.staffs.leavebooking.leave.ui.commands;

public record RejectHrLeaveRequestCommand(String leaveRequestId, String comment) {
    public RejectHrLeaveRequestCommand(String leaveRequestId) {
        this(leaveRequestId, null);
    }
}
