package uk.ac.staffs.leavebooking.common.events.integration;

import uk.ac.staffs.leavebooking.common.events.RemoteEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import static uk.ac.staffs.leavebooking.common.DomainAssertions.*;
import static uk.ac.staffs.leavebooking.common.events.integration.IntegrationEventAssertions.*;

public record LeaveRequestCancelledIntegrationEvent(
        Long id, LocalDate occurredOn, String leaveRequestId, String staffMemberId,
        LocalDate startDate, LocalDate endDate, String previousStatus,
        IntegrationLeaveType leaveType, IntegrationLeaveDayPortion dayPortion,
        BigDecimal chargedLeaveDays, IntegrationLeaveStatus status
) implements RemoteEvent {
    public LeaveRequestCancelledIntegrationEvent {
        occurredOn = argumentNotNull(occurredOn, "Event occurrence date cannot be null");
        leaveRequestId = argumentNotEmpty(leaveRequestId, "Leave request identity cannot be empty");
        staffMemberId = argumentNotEmpty(staffMemberId, "Staff member identity cannot be empty");
        validPeriod(startDate, endDate);
        previousStatus = argumentNotEmpty(previousStatus, "Previous leave status cannot be empty");
        leaveType = argumentNotNull(leaveType, "Leave type cannot be null");
        dayPortion = argumentNotNull(dayPortion, "Leave day portion cannot be null");
        chargedLeaveDays = nonNegativeDays(chargedLeaveDays);
        status = argumentNotNull(status, "Leave status cannot be null");
    }
    public LeaveRequestCancelledIntegrationEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId,
            LocalDate startDate, LocalDate endDate, String previousStatus,
            IntegrationLeaveType leaveType, IntegrationLeaveDayPortion dayPortion,
            BigDecimal chargedLeaveDays
    ) {
        this(null, occurredOn, leaveRequestId, staffMemberId, startDate, endDate,
                previousStatus, leaveType, dayPortion, chargedLeaveDays,
                IntegrationLeaveStatus.CANCELLED);
    }
    public LeaveRequestCancelledIntegrationEvent(
            Long id, LocalDate occurredOn, String leaveRequestId, String staffMemberId,
            LocalDate startDate, LocalDate endDate, String previousStatus
    ) {
        this(id, occurredOn, leaveRequestId, staffMemberId, startDate, endDate,
                previousStatus, IntegrationLeaveType.ANNUAL,
                IntegrationLeaveDayPortion.FULL_DAY,
                inclusiveCalendarDays(startDate, endDate),
                IntegrationLeaveStatus.CANCELLED);
    }
    public LeaveRequestCancelledIntegrationEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId,
            LocalDate startDate, LocalDate endDate, String previousStatus
    ) {
        this(occurredOn, leaveRequestId, staffMemberId, startDate, endDate, previousStatus,
                IntegrationLeaveType.ANNUAL, IntegrationLeaveDayPortion.FULL_DAY,
                inclusiveCalendarDays(startDate, endDate));
    }
    @Override public LeaveRequestCancelledIntegrationEvent withId(Long newId) {
        return new LeaveRequestCancelledIntegrationEvent(
                newId, occurredOn, leaveRequestId, staffMemberId, startDate, endDate,
                previousStatus, leaveType, dayPortion, chargedLeaveDays, status);
    }
}
