package uk.ac.staffs.leavebooking.leave.application.dto;

import uk.ac.staffs.leavebooking.leave.domain.LeaveDayPortion;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaveRequestSummaryDTO(
        String id,
        String staffMemberId,
        String managerId,
        LocalDate startDate,
        LocalDate endDate,
        LeaveType leaveType,
        LeaveDayPortion dayPortion,
        BigDecimal chargedLeaveDays,
        LeaveStatus status
) {
}
