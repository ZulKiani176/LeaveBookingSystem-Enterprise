package uk.ac.staffs.leavebooking.leave.application.dto;

import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;
import uk.ac.staffs.leavebooking.leave.domain.LeaveDayPortion;

import java.time.LocalDate;
import java.math.BigDecimal;

public record LeaveRequestDTO(
        String id,
        String staffMemberId,
        String managerId,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        LeaveType leaveType,
        LeaveDayPortion dayPortion,
        BigDecimal chargedLeaveDays,
        LeaveStatus status,
        String decisionComment
) {
    public LeaveRequestDTO(
            String id, String staffMemberId, String managerId,
            LocalDate startDate, LocalDate endDate, String reason,
            LeaveType leaveType, LeaveStatus status
    ) {
        this(
                id, staffMemberId, managerId, startDate, endDate, reason, leaveType,
                LeaveDayPortion.FULL_DAY,
                leaveType == LeaveType.SICK ? BigDecimal.ZERO : BigDecimal.valueOf(
                        new uk.ac.staffs.leavebooking.leave.domain.LeavePeriod(
                                startDate, endDate
                        ).calendarDays()
                ),
                status, null
        );
    }
}
