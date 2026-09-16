package uk.ac.staffs.leavebooking.common.events.integration;

import uk.ac.staffs.leavebooking.common.events.RemoteEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import static uk.ac.staffs.leavebooking.common.DomainAssertions.*;
import static uk.ac.staffs.leavebooking.common.events.integration.IntegrationEventAssertions.*;

public record LeaveRequestRejectedIntegrationEvent(
        Long id, LocalDate occurredOn, String leaveRequestId, String staffMemberId,
        LocalDate startDate, LocalDate endDate, IntegrationLeaveType leaveType,
        IntegrationLeaveDayPortion dayPortion, BigDecimal chargedLeaveDays,
        IntegrationLeaveStatus status, IntegrationLeaveStatus previousStatus
) implements RemoteEvent {
    public LeaveRequestRejectedIntegrationEvent {
        occurredOn = argumentNotNull(occurredOn, "Event occurrence date cannot be null");
        leaveRequestId = argumentNotEmpty(leaveRequestId, "Leave request identity cannot be empty");
        staffMemberId = argumentNotEmpty(staffMemberId, "Staff member identity cannot be empty");
        validPeriod(startDate, endDate);
        leaveType = argumentNotNull(leaveType, "Leave type cannot be null");
        dayPortion = argumentNotNull(dayPortion, "Leave day portion cannot be null");
        chargedLeaveDays = nonNegativeDays(chargedLeaveDays);
        status = argumentNotNull(status, "Leave status cannot be null");
    }
    public LeaveRequestRejectedIntegrationEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId,
            LocalDate startDate, LocalDate endDate, IntegrationLeaveType leaveType,
            IntegrationLeaveDayPortion dayPortion, BigDecimal chargedLeaveDays
    ) {
        this(null, occurredOn, leaveRequestId, staffMemberId, startDate, endDate,
                leaveType, dayPortion, chargedLeaveDays, IntegrationLeaveStatus.REJECTED);
    }

    public LeaveRequestRejectedIntegrationEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId,
            LocalDate startDate, LocalDate endDate, IntegrationLeaveType leaveType,
            IntegrationLeaveDayPortion dayPortion, BigDecimal chargedLeaveDays,
            IntegrationLeaveStatus previousStatus
    ) {
        this(null, occurredOn, leaveRequestId, staffMemberId, startDate, endDate,
                leaveType, dayPortion, chargedLeaveDays, IntegrationLeaveStatus.REJECTED,
                previousStatus);
    }
    public LeaveRequestRejectedIntegrationEvent(
            Long id, LocalDate occurredOn, String leaveRequestId, String staffMemberId
    ) {
        this(id, occurredOn, leaveRequestId, staffMemberId, LocalDate.of(1970, 1, 1),
                LocalDate.of(1970, 1, 1), IntegrationLeaveType.ANNUAL,
                IntegrationLeaveDayPortion.FULL_DAY, BigDecimal.ONE,
                IntegrationLeaveStatus.REJECTED, IntegrationLeaveStatus.PENDING);
    }
    public LeaveRequestRejectedIntegrationEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId
    ) {
        this(occurredOn, leaveRequestId, staffMemberId, LocalDate.of(1970, 1, 1),
                LocalDate.of(1970, 1, 1), IntegrationLeaveType.ANNUAL,
                IntegrationLeaveDayPortion.FULL_DAY,
                BigDecimal.ONE);
    }
    @Override public LeaveRequestRejectedIntegrationEvent withId(Long newId) {
        return new LeaveRequestRejectedIntegrationEvent(
                newId, occurredOn, leaveRequestId, staffMemberId, startDate, endDate,
                leaveType, dayPortion, chargedLeaveDays, status, previousStatus);
    }

    public LeaveRequestRejectedIntegrationEvent(
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
