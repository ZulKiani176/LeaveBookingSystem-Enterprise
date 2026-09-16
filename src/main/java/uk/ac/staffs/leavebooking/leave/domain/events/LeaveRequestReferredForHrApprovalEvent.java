package uk.ac.staffs.leavebooking.leave.domain.events;

import uk.ac.staffs.leavebooking.common.events.LocalEvent;

import java.time.LocalDate;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotEmpty;
import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotNull;

public record LeaveRequestReferredForHrApprovalEvent(
        Long id,
        LocalDate occurredOn,
        String leaveRequestId,
        String staffMemberId
) implements LocalEvent {
    public static final String OCCURRED_ON_NOT_NULL = "Event occurrence date cannot be null";
    public static final String LEAVE_REQUEST_ID_NOT_EMPTY = "Leave request identity cannot be empty";
    public static final String STAFF_MEMBER_ID_NOT_EMPTY = "Staff member identity cannot be empty";

    public LeaveRequestReferredForHrApprovalEvent {
        occurredOn = argumentNotNull(occurredOn, OCCURRED_ON_NOT_NULL);
        leaveRequestId = argumentNotEmpty(leaveRequestId, LEAVE_REQUEST_ID_NOT_EMPTY);
        staffMemberId = argumentNotEmpty(staffMemberId, STAFF_MEMBER_ID_NOT_EMPTY);
    }

    public LeaveRequestReferredForHrApprovalEvent(
            LocalDate occurredOn,
            String leaveRequestId,
            String staffMemberId
    ) {
        this(null, occurredOn, leaveRequestId, staffMemberId);
    }

    @Override
    public LeaveRequestReferredForHrApprovalEvent withId(Long newId) {
        return new LeaveRequestReferredForHrApprovalEvent(
                newId,
                occurredOn,
                leaveRequestId,
                staffMemberId
        );
    }
}
