package uk.ac.staffs.leavebooking.leave.application.mappers;

import uk.ac.staffs.leavebooking.leave.domain.LeaveRequest;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveRequestJpa;

import java.util.Objects;

public final class LeaveRequestDomainToJpaMapper {
    public static final String LEAVE_REQUEST_NOT_NULL = "Leave request cannot be null";

    private LeaveRequestDomainToJpaMapper() {
    }

    public static LeaveRequestJpa map(LeaveRequest leaveRequest) {
        return map(leaveRequest, null);
    }

    public static LeaveRequestJpa map(LeaveRequest leaveRequest, Long version) {
        Objects.requireNonNull(leaveRequest, LEAVE_REQUEST_NOT_NULL);

        return new LeaveRequestJpa(
                leaveRequest.id().id(),
                leaveRequest.staffMemberId(),
                leaveRequest.managerId(),
                leaveRequest.leavePeriod().startDate(),
                leaveRequest.leavePeriod().endDate(),
                leaveRequest.reason(),
                leaveRequest.leaveType(),
                leaveRequest.dayPortion(),
                leaveRequest.chargedLeaveDays().value(),
                leaveRequest.status(),
                leaveRequest.decisionComment(),
                version
        );
    }
}
