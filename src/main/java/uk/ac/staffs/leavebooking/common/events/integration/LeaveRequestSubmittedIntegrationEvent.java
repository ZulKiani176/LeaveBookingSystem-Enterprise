package uk.ac.staffs.leavebooking.common.events.integration;

import uk.ac.staffs.leavebooking.common.events.RemoteEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import static uk.ac.staffs.leavebooking.common.DomainAssertions.*;
import static uk.ac.staffs.leavebooking.common.events.integration.IntegrationEventAssertions.*;

public record LeaveRequestSubmittedIntegrationEvent(
        Long id, LocalDate occurredOn, String leaveRequestId, String staffMemberId,
        String managerId, LocalDate startDate, LocalDate endDate,
        IntegrationLeaveType leaveType, IntegrationLeaveDayPortion dayPortion,
        BigDecimal chargedLeaveDays, IntegrationLeaveStatus status
) implements RemoteEvent {
    public LeaveRequestSubmittedIntegrationEvent {
        occurredOn = argumentNotNull(occurredOn, "Event occurrence date cannot be null");
        leaveRequestId = argumentNotEmpty(leaveRequestId, "Leave request identity cannot be empty");
        staffMemberId = argumentNotEmpty(staffMemberId, "Staff member identity cannot be empty");
        managerId = argumentNotEmpty(managerId, "Manager identity cannot be empty");
        validPeriod(startDate, endDate);
        leaveType = argumentNotNull(leaveType, "Leave type cannot be null");
        dayPortion = argumentNotNull(dayPortion, "Leave day portion cannot be null");
        chargedLeaveDays = nonNegativeDays(chargedLeaveDays);
        status = argumentNotNull(status, "Leave status cannot be null");
    }
    public LeaveRequestSubmittedIntegrationEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId,
            String managerId, LocalDate startDate, LocalDate endDate,
            IntegrationLeaveType leaveType, IntegrationLeaveDayPortion dayPortion,
            BigDecimal chargedLeaveDays
    ) {
        this(null, occurredOn, leaveRequestId, staffMemberId, managerId, startDate, endDate,
                leaveType, dayPortion, chargedLeaveDays, IntegrationLeaveStatus.PENDING);
    }
    public LeaveRequestSubmittedIntegrationEvent(
            Long id, LocalDate occurredOn, String leaveRequestId, String staffMemberId,
            String managerId, LocalDate startDate, LocalDate endDate
    ) {
        this(id, occurredOn, leaveRequestId, staffMemberId, managerId, startDate, endDate,
                IntegrationLeaveType.ANNUAL, IntegrationLeaveDayPortion.FULL_DAY,
                inclusiveCalendarDays(startDate, endDate), IntegrationLeaveStatus.PENDING);
    }
    public LeaveRequestSubmittedIntegrationEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId,
            String managerId, LocalDate startDate, LocalDate endDate
    ) {
        this(occurredOn, leaveRequestId, staffMemberId, managerId, startDate, endDate,
                IntegrationLeaveType.ANNUAL, IntegrationLeaveDayPortion.FULL_DAY,
                inclusiveCalendarDays(startDate, endDate));
    }
    @Override public LeaveRequestSubmittedIntegrationEvent withId(Long newId) {
        return new LeaveRequestSubmittedIntegrationEvent(
                newId, occurredOn, leaveRequestId, staffMemberId, managerId, startDate, endDate,
                leaveType, dayPortion, chargedLeaveDays, status);
    }
}
