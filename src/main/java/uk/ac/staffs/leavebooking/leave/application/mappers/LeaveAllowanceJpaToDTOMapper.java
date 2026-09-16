package uk.ac.staffs.leavebooking.leave.application.mappers;

import uk.ac.staffs.leavebooking.leave.application.dto.LeaveAllowanceDTO;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceJpa;

import java.util.Objects;

public final class LeaveAllowanceJpaToDTOMapper {
    public static final String LEAVE_ALLOWANCE_JPA_NOT_NULL = "Leave allowance JPA entity cannot be null";

    private LeaveAllowanceJpaToDTOMapper() {
    }

    public static LeaveAllowanceDTO map(LeaveAllowanceJpa leaveAllowanceJpa) {
        Objects.requireNonNull(leaveAllowanceJpa, LEAVE_ALLOWANCE_JPA_NOT_NULL);

        return new LeaveAllowanceDTO(
                leaveAllowanceJpa.getId(),
                leaveAllowanceJpa.getStaffMemberId(),
                leaveAllowanceJpa.getFirstName(),
                leaveAllowanceJpa.getSurname(),
                leaveAllowanceJpa.getManagerId(),
                leaveAllowanceJpa.getBusinessYearStart(),
                leaveAllowanceJpa.getBusinessYearEnd(),
                leaveAllowanceJpa.getBaseEntitlement(),
                leaveAllowanceJpa.getCarriedOverDays(),
                leaveAllowanceJpa.getTotalEntitlement(),
                leaveAllowanceJpa.getRemainingLeaveDays(),
                leaveAllowanceJpa.getTotalEntitlement().subtract(
                        leaveAllowanceJpa.getRemainingLeaveDays()
                )
        );
    }
}
