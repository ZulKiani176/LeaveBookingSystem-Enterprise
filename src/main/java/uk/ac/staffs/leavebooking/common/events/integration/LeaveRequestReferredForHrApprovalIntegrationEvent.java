package uk.ac.staffs.leavebooking.common.events.integration;

import uk.ac.staffs.leavebooking.common.events.RemoteEvent;

import java.time.LocalDate;
import java.math.BigDecimal;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotEmpty;
import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotNull;

public record LeaveRequestReferredForHrApprovalIntegrationEvent(
        Long id,
        LocalDate occurredOn,
        String leaveRequestId,
        String staffMemberId,
        IntegrationLeaveStatus previousStatus,
        IntegrationLeaveStatus status,
        LocalDate startDate,
        LocalDate endDate,
        IntegrationLeaveType leaveType,
        IntegrationLeaveDayPortion dayPortion,
        BigDecimal chargedLeaveDays
) implements RemoteEvent {
    public LeaveRequestReferredForHrApprovalIntegrationEvent {
        occurredOn = argumentNotNull(occurredOn, "Event occurrence date cannot be null");
        leaveRequestId = argumentNotEmpty(leaveRequestId, "Leave request identity cannot be empty");
        staffMemberId = argumentNotEmpty(staffMemberId, "Staff member identity cannot be empty");
        if (previousStatus != IntegrationLeaveStatus.PENDING
                || status != IntegrationLeaveStatus.PENDING_HR_APPROVAL) {
            throw new IllegalArgumentException("Invalid HR referral integration event state");
        }
        if (startDate != null || endDate != null || leaveType != null
                || dayPortion != null || chargedLeaveDays != null) {
            IntegrationEventAssertions.validPeriod(startDate, endDate);
            leaveType = argumentNotNull(leaveType, "Leave type cannot be null");
            dayPortion = argumentNotNull(dayPortion, "Leave day portion cannot be null");
            chargedLeaveDays = IntegrationEventAssertions.nonNegativeDays(chargedLeaveDays);
        }
    }

    public LeaveRequestReferredForHrApprovalIntegrationEvent(
            LocalDate occurredOn,
            String leaveRequestId,
            String staffMemberId
    ) {
        this(null, occurredOn, leaveRequestId, staffMemberId,
                IntegrationLeaveStatus.PENDING,
                IntegrationLeaveStatus.PENDING_HR_APPROVAL);
    }

    @Override
    public LeaveRequestReferredForHrApprovalIntegrationEvent withId(Long newId) {
        return new LeaveRequestReferredForHrApprovalIntegrationEvent(
                newId, occurredOn, leaveRequestId, staffMemberId, previousStatus, status,
                startDate, endDate, leaveType, dayPortion, chargedLeaveDays
        );
    }

    public LeaveRequestReferredForHrApprovalIntegrationEvent(
            Long id, LocalDate occurredOn, String leaveRequestId, String staffMemberId,
            IntegrationLeaveStatus previousStatus, IntegrationLeaveStatus status
    ) {
        this(id, occurredOn, leaveRequestId, staffMemberId, previousStatus, status,
                null, null, null, null, null);
    }
}
