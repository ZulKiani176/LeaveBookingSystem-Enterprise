package uk.ac.staffs.leavebooking.leave.ui.commands;

public record RejectLeaveRequestCommand(
        String leaveRequestId,
        String comment
) {
    public RejectLeaveRequestCommand(String leaveRequestId) {
        this(leaveRequestId, null);
    }
}
