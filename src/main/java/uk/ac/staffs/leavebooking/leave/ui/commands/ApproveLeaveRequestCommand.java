package uk.ac.staffs.leavebooking.leave.ui.commands;

public record ApproveLeaveRequestCommand(
        String leaveRequestId,
        String comment
) {
    public ApproveLeaveRequestCommand(String leaveRequestId) {
        this(leaveRequestId, null);
    }
}
