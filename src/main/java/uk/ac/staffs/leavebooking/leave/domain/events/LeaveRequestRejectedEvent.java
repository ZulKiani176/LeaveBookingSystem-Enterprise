package uk.ac.staffs.leavebooking.leave.domain.events;

import uk.ac.staffs.leavebooking.common.events.LocalEvent;

import java.time.LocalDate;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotEmpty;
import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotNull;

public record LeaveRequestRejectedEvent(
        Long id,
        LocalDate occurredOn,
        String leaveRequestId,
        String staffMemberId,
        String decisionComment
) implements LocalEvent {
    public LeaveRequestRejectedEvent {
        occurredOn = argumentNotNull(occurredOn, "Event occurrence date cannot be null");
        leaveRequestId = argumentNotEmpty(leaveRequestId, "Leave request identity cannot be empty");
        staffMemberId = argumentNotEmpty(staffMemberId, "Staff member identity cannot be empty");
    }

    public LeaveRequestRejectedEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId, String decisionComment
    ) {
        this(null, occurredOn, leaveRequestId, staffMemberId, decisionComment);
    }

    public LeaveRequestRejectedEvent(
            LocalDate occurredOn, String leaveRequestId, String staffMemberId
    ) {
        this(occurredOn, leaveRequestId, staffMemberId, null);
    }

    @Override
    public LeaveRequestRejectedEvent withId(Long newId) {
        return new LeaveRequestRejectedEvent(
                newId, occurredOn, leaveRequestId, staffMemberId, decisionComment
        );
    }
}
