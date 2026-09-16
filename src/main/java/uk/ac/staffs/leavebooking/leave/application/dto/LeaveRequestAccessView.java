package uk.ac.staffs.leavebooking.leave.application.dto;

public record LeaveRequestAccessView(
        String staffMemberId,
        String managerId,
        String leaveType,
        String status
) {
}
