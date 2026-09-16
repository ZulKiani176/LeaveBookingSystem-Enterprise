package uk.ac.staffs.leavebooking.leave.domain.events;

import uk.ac.staffs.leavebooking.common.events.LocalEvent;
import uk.ac.staffs.leavebooking.leave.domain.LeaveDays;
import uk.ac.staffs.leavebooking.leave.domain.LeavePeriod;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotEmpty;
import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotNull;

public record LeaveRequestCancelledEvent(
        Long id,
        LocalDate occurredOn,
        String leaveRequestId,
        String staffMemberId,
        LocalDate startDate,
        LocalDate endDate,
        LeaveStatus previousStatus,
        BigDecimal chargedLeaveDays
) implements LocalEvent {
    public LeaveRequestCancelledEvent {
        occurredOn = argumentNotNull(occurredOn, "Event occurrence date cannot be null");
        leaveRequestId = argumentNotEmpty(leaveRequestId, "Leave request identity cannot be empty");
        staffMemberId = argumentNotEmpty(staffMemberId, "Staff member identity cannot be empty");
        new LeavePeriod(startDate, endDate);
        previousStatus = argumentNotNull(previousStatus, "Previous leave status cannot be null");
        chargedLeaveDays = new LeaveDays(chargedLeaveDays).value();
    }

    public LeaveRequestCancelledEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId,
            LocalDate startDate, LocalDate endDate, LeaveStatus previousStatus,
            BigDecimal chargedLeaveDays
    ) {
        this(null, occurredOn, leaveRequestId, staffMemberId, startDate, endDate,
                previousStatus, chargedLeaveDays);
    }

    public LeaveRequestCancelledEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId,
            LocalDate startDate, LocalDate endDate, LeaveStatus previousStatus
    ) {
        this(occurredOn, leaveRequestId, staffMemberId, startDate, endDate, previousStatus,
                BigDecimal.valueOf(new LeavePeriod(startDate, endDate).calendarDays()));
    }

    @Override
    public LeaveRequestCancelledEvent withId(Long newId) {
        return new LeaveRequestCancelledEvent(
                newId, occurredOn, leaveRequestId, staffMemberId, startDate, endDate,
                previousStatus, chargedLeaveDays
        );
    }
}
