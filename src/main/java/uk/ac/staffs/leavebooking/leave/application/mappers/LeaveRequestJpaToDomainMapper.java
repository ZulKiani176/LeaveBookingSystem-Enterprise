package uk.ac.staffs.leavebooking.leave.application.mappers;

import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.leave.domain.LeavePeriod;
import uk.ac.staffs.leavebooking.leave.domain.LeaveRequest;
import uk.ac.staffs.leavebooking.leave.domain.LeaveDays;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveRequestJpa;

import java.util.Objects;

public final class LeaveRequestJpaToDomainMapper {
    public static final String LEAVE_REQUEST_JPA_NOT_NULL = "Leave request JPA entity cannot be null";

    private LeaveRequestJpaToDomainMapper() {
    }

    public static LeaveRequest map(LeaveRequestJpa leaveRequestJpa) {
        Objects.requireNonNull(leaveRequestJpa, LEAVE_REQUEST_JPA_NOT_NULL);

        Identity<LeaveRequest> id = Identity.of(leaveRequestJpa.getId());
        LeavePeriod leavePeriod = new LeavePeriod(
                leaveRequestJpa.getStartDate(),
                leaveRequestJpa.getEndDate()
        );

        return LeaveRequest.reconstitute(
                id,
                leaveRequestJpa.getStaffMemberId(),
                leaveRequestJpa.getManagerId(),
                leavePeriod,
                leaveRequestJpa.getReason(),
                leaveRequestJpa.getLeaveType(),
                leaveRequestJpa.getDayPortion(),
                new LeaveDays(leaveRequestJpa.getChargedLeaveDays()),
                leaveRequestJpa.getStatus(),
                leaveRequestJpa.getDecisionComment()
        );
    }
}
