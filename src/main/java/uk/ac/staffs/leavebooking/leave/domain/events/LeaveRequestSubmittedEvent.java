package uk.ac.staffs.leavebooking.leave.domain.events;

import uk.ac.staffs.leavebooking.common.events.LocalEvent;
import uk.ac.staffs.leavebooking.leave.domain.LeaveDayPortion;
import uk.ac.staffs.leavebooking.leave.domain.LeaveDays;
import uk.ac.staffs.leavebooking.leave.domain.LeavePeriod;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotEmpty;
import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotNull;

public record LeaveRequestSubmittedEvent(
        Long id,
        LocalDate occurredOn,
        String leaveRequestId,
        String staffMemberId,
        String managerId,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        LeaveType leaveType,
        LeaveDayPortion dayPortion,
        BigDecimal chargedLeaveDays
) implements LocalEvent {
    public LeaveRequestSubmittedEvent {
        occurredOn = argumentNotNull(occurredOn, "Event occurrence date cannot be null");
        leaveRequestId = argumentNotEmpty(leaveRequestId, "Leave request identity cannot be empty");
        staffMemberId = argumentNotEmpty(staffMemberId, "Staff member identity cannot be empty");
        managerId = argumentNotEmpty(managerId, "Manager identity cannot be empty");
        new LeavePeriod(startDate, endDate);
        reason = argumentNotEmpty(reason, "Leave reason cannot be empty");
        leaveType = argumentNotNull(leaveType, "Leave type cannot be null");
        dayPortion = argumentNotNull(dayPortion, "Leave day portion cannot be null");
        chargedLeaveDays = new LeaveDays(chargedLeaveDays).value();
    }

    public LeaveRequestSubmittedEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId, String managerId,
            LocalDate startDate, LocalDate endDate, String reason, LeaveType leaveType,
            LeaveDayPortion dayPortion, BigDecimal chargedLeaveDays
    ) {
        this(null, occurredOn, leaveRequestId, staffMemberId, managerId, startDate, endDate,
                reason, leaveType, dayPortion, chargedLeaveDays);
    }

    public LeaveRequestSubmittedEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId, String managerId,
            LocalDate startDate, LocalDate endDate
    ) {
        this(occurredOn, leaveRequestId, staffMemberId, managerId, startDate, endDate,
                "Annual leave", LeaveType.ANNUAL, LeaveDayPortion.FULL_DAY,
                BigDecimal.valueOf(new LeavePeriod(startDate, endDate).calendarDays()));
    }

    @Override
    public LeaveRequestSubmittedEvent withId(Long newId) {
        return new LeaveRequestSubmittedEvent(
                newId, occurredOn, leaveRequestId, staffMemberId, managerId, startDate, endDate,
                reason, leaveType, dayPortion, chargedLeaveDays
        );
    }
}
