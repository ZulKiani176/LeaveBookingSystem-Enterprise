package uk.ac.staffs.leavebooking.reporting.application.dto;

import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveDayPortion;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveStatus;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaveReportingDTO(
        String leaveRequestId,
        Long sourceEventId,
        String staffMemberId,
        IntegrationLeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        IntegrationLeaveDayPortion dayPortion,
        BigDecimal chargedLeaveDays,
        IntegrationLeaveStatus status,
        LocalDate occurredOn
) {
}
