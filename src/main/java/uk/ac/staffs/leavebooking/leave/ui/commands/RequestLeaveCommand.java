package uk.ac.staffs.leavebooking.leave.ui.commands;

import uk.ac.staffs.leavebooking.leave.domain.LeaveType;
import uk.ac.staffs.leavebooking.leave.domain.LeaveDayPortion;

import java.time.LocalDate;

public record RequestLeaveCommand(
        String staffMemberId,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        LeaveType leaveType,
        LeaveDayPortion dayPortion
) {
    public RequestLeaveCommand(
            String staffMemberId, LocalDate startDate, LocalDate endDate,
            String reason, LeaveType leaveType
    ) {
        this(staffMemberId, startDate, endDate, reason, leaveType, LeaveDayPortion.FULL_DAY);
    }
}
