package uk.ac.staffs.leavebooking.leave.application.mappers;

import uk.ac.staffs.leavebooking.leave.application.dto.LeaveRequestSummaryDTO;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveRequestJpa;

import java.util.Objects;

public final class LeaveRequestJpaToSummaryMapper {
    private LeaveRequestJpaToSummaryMapper() {
    }

    public static LeaveRequestSummaryDTO map(LeaveRequestJpa request) {
        Objects.requireNonNull(request, "Leave request JPA entity cannot be null");
        return new LeaveRequestSummaryDTO(
                request.getId(),
                request.getStaffMemberId(),
                request.getManagerId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getLeaveType(),
                request.getDayPortion(),
                request.getChargedLeaveDays(),
                request.getStatus()
        );
    }
}
