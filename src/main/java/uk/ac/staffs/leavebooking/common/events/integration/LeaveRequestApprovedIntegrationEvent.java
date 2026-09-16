package uk.ac.staffs.leavebooking.common.events.integration;

import uk.ac.staffs.leavebooking.common.events.RemoteEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import static uk.ac.staffs.leavebooking.common.DomainAssertions.*;
import static uk.ac.staffs.leavebooking.common.events.integration.IntegrationEventAssertions.*;

public record LeaveRequestApprovedIntegrationEvent(
        Long id, LocalDate occurredOn, String leaveRequestId, String staffMemberId,
        LocalDate startDate, LocalDate endDate, IntegrationLeaveType leaveType,
        IntegrationLeaveDayPortion dayPortion, BigDecimal chargedLeaveDays,
        IntegrationLeaveStatus status, IntegrationLeaveStatus previousStatus
) implements RemoteEvent {
    public LeaveRequestApprovedIntegrationEvent {
        occurredOn = argumentNotNull(occurredOn, "Event occurrence date cannot be null");
        leaveRequestId = argumentNotEmpty(leaveRequestId, "Leave request identity cannot be empty");
        staffMemberId = argumentNotEmpty(staffMemberId, "Staff member identity cannot be empty");
        validPeriod(startDate, endDate);
        leaveType = argumentNotNull(leaveType, "Leave type cannot be null");
        dayPortion = argumentNotNull(dayPortion, "Leave day portion cannot be null");
        chargedLeaveDays = nonNegativeDays(chargedLeaveDays);
        status = argumentNotNull(status, "Leave status cannot be null");
    }
    public LeaveRequestApprovedIntegrationEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId,
            LocalDate startDate, LocalDate endDate, IntegrationLeaveType leaveType,
            IntegrationLeaveDayPortion dayPortion, BigDecimal chargedLeaveDays
    ) {
        this(null, occurredOn, leaveRequestId, staffMemberId, startDate, endDate,
                leaveType, dayPortion, chargedLeaveDays, IntegrationLeaveStatus.APPROVED);
    }

    public LeaveRequestApprovedIntegrationEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId,
            LocalDate startDate, LocalDate endDate, IntegrationLeaveType leaveType,
            IntegrationLeaveDayPortion dayPortion, BigDecimal chargedLeaveDays,
            IntegrationLeaveStatus previousStatus
    ) {
        this(null, occurredOn, leaveRequestId, staffMemberId, startDate, endDate,
                leaveType, dayPortion, chargedLeaveDays, IntegrationLeaveStatus.APPROVED,
                previousStatus);
    }
    public LeaveRequestApprovedIntegrationEvent(
            Long id, LocalDate occurredOn, String leaveRequestId, String staffMemberId,
            LocalDate startDate, LocalDate endDate
    ) {
        this(id, occurredOn, leaveRequestId, staffMemberId, startDate, endDate,
                IntegrationLeaveType.ANNUAL, IntegrationLeaveDayPortion.FULL_DAY,
                inclusiveCalendarDays(startDate, endDate),
                IntegrationLeaveStatus.APPROVED, IntegrationLeaveStatus.PENDING);
    }
    public LeaveRequestApprovedIntegrationEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId,
            LocalDate startDate, LocalDate endDate
    ) {
        this(occurredOn, leaveRequestId, staffMemberId, startDate, endDate,
                IntegrationLeaveType.ANNUAL, IntegrationLeaveDayPortion.FULL_DAY,
                inclusiveCalendarDays(startDate, endDate));
    }
    @Override public LeaveRequestApprovedIntegrationEvent withId(Long newId) {
        return new LeaveRequestApprovedIntegrationEvent(
                newId, occurredOn, leaveRequestId, staffMemberId, startDate, endDate,
                leaveType, dayPortion, chargedLeaveDays, status, previousStatus);
    }

    public LeaveRequestApprovedIntegrationEvent(
            Long id, LocalDate occurredOn, String leaveRequestId, String staffMemberId,
            LocalDate startDate, LocalDate endDate, IntegrationLeaveType leaveType,
            IntegrationLeaveDayPortion dayPortion, BigDecimal chargedLeaveDays,
            IntegrationLeaveStatus status
    ) {
        this(id, occurredOn, leaveRequestId, staffMemberId, startDate, endDate,
                leaveType, dayPortion, chargedLeaveDays, status,
                IntegrationLeaveStatus.PENDING);
    }
}
