package uk.ac.staffs.leavebooking.leave.domain.events;

import uk.ac.staffs.leavebooking.common.events.LocalEvent;
import uk.ac.staffs.leavebooking.leave.domain.LeaveDays;
import uk.ac.staffs.leavebooking.leave.domain.LeavePeriod;

import java.math.BigDecimal;
import java.time.LocalDate;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotEmpty;
import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotNull;

public record LeaveRequestApprovedEvent(
        Long id,
        LocalDate occurredOn,
        String leaveRequestId,
        String staffMemberId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal chargedLeaveDays,
        String decisionComment
) implements LocalEvent {
    public LeaveRequestApprovedEvent {
        occurredOn = argumentNotNull(occurredOn, "Event occurrence date cannot be null");
        leaveRequestId = argumentNotEmpty(leaveRequestId, "Leave request identity cannot be empty");
        staffMemberId = argumentNotEmpty(staffMemberId, "Staff member identity cannot be empty");
        new LeavePeriod(startDate, endDate);
        chargedLeaveDays = new LeaveDays(chargedLeaveDays).value();
    }

    public LeaveRequestApprovedEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId,
            LocalDate startDate, LocalDate endDate, BigDecimal chargedLeaveDays,
            String decisionComment
    ) {
        this(null, occurredOn, leaveRequestId, staffMemberId, startDate, endDate,
                chargedLeaveDays, decisionComment);
    }

    public LeaveRequestApprovedEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId,
            LocalDate startDate, LocalDate endDate
    ) {
        this(occurredOn, leaveRequestId, staffMemberId, startDate, endDate,
                BigDecimal.valueOf(new LeavePeriod(startDate, endDate).calendarDays()), null);
    }

    @Override
    public LeaveRequestApprovedEvent withId(Long newId) {
        return new LeaveRequestApprovedEvent(
                newId, occurredOn, leaveRequestId, staffMemberId, startDate, endDate,
                chargedLeaveDays, decisionComment
        );
    }
}
