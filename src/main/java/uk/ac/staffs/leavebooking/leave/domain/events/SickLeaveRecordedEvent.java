package uk.ac.staffs.leavebooking.leave.domain.events;

import uk.ac.staffs.leavebooking.common.events.LocalEvent;
import uk.ac.staffs.leavebooking.leave.domain.LeavePeriod;

import java.time.LocalDate;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotEmpty;
import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotNull;

public record SickLeaveRecordedEvent(
        Long id,
        LocalDate occurredOn,
        String leaveRequestId,
        String staffMemberId,
        String managerId,
        LocalDate startDate,
        LocalDate endDate,
        String reason
) implements LocalEvent {
    public SickLeaveRecordedEvent {
        occurredOn = argumentNotNull(occurredOn, "Event occurrence date cannot be null");
        leaveRequestId = argumentNotEmpty(leaveRequestId, "Leave request identity cannot be empty");
        staffMemberId = argumentNotEmpty(staffMemberId, "Staff member identity cannot be empty");
        managerId = argumentNotEmpty(managerId, "Manager identity cannot be empty");
        new LeavePeriod(startDate, endDate);
        reason = argumentNotEmpty(reason, "Sickness reason cannot be empty");
    }

    public SickLeaveRecordedEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId, String managerId,
            LocalDate startDate, LocalDate endDate, String reason
    ) {
        this(null, occurredOn, leaveRequestId, staffMemberId, managerId, startDate, endDate, reason);
    }

    @Override
    public SickLeaveRecordedEvent withId(Long newId) {
        return new SickLeaveRecordedEvent(
                newId, occurredOn, leaveRequestId, staffMemberId, managerId,
                startDate, endDate, reason
        );
    }
}
