package uk.ac.staffs.leavebooking.leave.application.mappers;

import uk.ac.staffs.leavebooking.leave.application.dto.LeaveRequestDTO;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveRequestJpa;

import java.util.Objects;

public final class LeaveRequestJpaToDTOMapper {
    public static final String LEAVE_REQUEST_JPA_NOT_NULL = "Leave request JPA entity cannot be null";

    private LeaveRequestJpaToDTOMapper() {
    }

    public static LeaveRequestDTO map(LeaveRequestJpa leaveRequestJpa) {
        Objects.requireNonNull(leaveRequestJpa, LEAVE_REQUEST_JPA_NOT_NULL);

        return new LeaveRequestDTO(
                leaveRequestJpa.getId(),
                leaveRequestJpa.getStaffMemberId(),
                leaveRequestJpa.getManagerId(),
                leaveRequestJpa.getStartDate(),
                leaveRequestJpa.getEndDate(),
                leaveRequestJpa.getReason(),
                leaveRequestJpa.getLeaveType(),
                leaveRequestJpa.getDayPortion(),
                leaveRequestJpa.getChargedLeaveDays(),
                leaveRequestJpa.getStatus(),
                leaveRequestJpa.getDecisionComment()
        );
    }
}
