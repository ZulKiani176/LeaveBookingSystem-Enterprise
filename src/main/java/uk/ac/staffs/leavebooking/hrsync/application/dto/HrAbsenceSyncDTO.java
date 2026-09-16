package uk.ac.staffs.leavebooking.hrsync.application.dto;

import uk.ac.staffs.leavebooking.hrsync.domain.HrAbsenceSyncAction;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveDayPortion;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveStatus;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HrAbsenceSyncDTO(
        Long sourceEventId,
        String leaveRequestId,
        String staffMemberId,
        HrAbsenceSyncAction syncAction,
        IntegrationLeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        IntegrationLeaveDayPortion dayPortion,
        BigDecimal chargedLeaveDays,
        IntegrationLeaveStatus status,
        LocalDate occurredOn
) {
}
