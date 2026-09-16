package uk.ac.staffs.leavebooking.common.events.integration;

import uk.ac.staffs.leavebooking.common.events.RemoteEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import static uk.ac.staffs.leavebooking.common.DomainAssertions.*;
import static uk.ac.staffs.leavebooking.common.events.integration.IntegrationEventAssertions.*;

public record SickLeaveRecordedIntegrationEvent(
        Long id, LocalDate occurredOn, String leaveRequestId, String staffMemberId,
        String managerId, LocalDate startDate, LocalDate endDate,
        IntegrationLeaveType leaveType, IntegrationLeaveDayPortion dayPortion,
        BigDecimal chargedLeaveDays, IntegrationLeaveStatus status
) implements RemoteEvent {
    public SickLeaveRecordedIntegrationEvent {
        occurredOn = argumentNotNull(occurredOn, "Event occurrence date cannot be null");
        leaveRequestId = argumentNotEmpty(leaveRequestId, "Leave request identity cannot be empty");
        staffMemberId = argumentNotEmpty(staffMemberId, "Staff member identity cannot be empty");
        managerId = argumentNotEmpty(managerId, "Manager identity cannot be empty");
        validPeriod(startDate, endDate);
        chargedLeaveDays = nonNegativeDays(chargedLeaveDays);
        if (leaveType != IntegrationLeaveType.SICK
                || dayPortion != IntegrationLeaveDayPortion.FULL_DAY
                || chargedLeaveDays.compareTo(BigDecimal.ZERO) != 0
                || status != IntegrationLeaveStatus.RECORDED) {
            throw new IllegalArgumentException("Invalid sickness integration event state");
        }
    }
    public SickLeaveRecordedIntegrationEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId,
            String managerId, LocalDate startDate, LocalDate endDate
    ) {
        this(null, occurredOn, leaveRequestId, staffMemberId, managerId, startDate, endDate,
                IntegrationLeaveType.SICK, IntegrationLeaveDayPortion.FULL_DAY,
                BigDecimal.ZERO, IntegrationLeaveStatus.RECORDED);
    }
    @Override public SickLeaveRecordedIntegrationEvent withId(Long newId) {
        return new SickLeaveRecordedIntegrationEvent(
                newId, occurredOn, leaveRequestId, staffMemberId, managerId,
                startDate, endDate, leaveType, dayPortion, chargedLeaveDays, status);
    }
}
