package uk.ac.staffs.leavebooking.leave.application.mappers;

import uk.ac.staffs.leavebooking.common.FullName;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.leave.domain.BusinessYear;
import uk.ac.staffs.leavebooking.leave.domain.LeaveAllowance;
import uk.ac.staffs.leavebooking.leave.domain.LeaveDays;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceJpa;

import java.util.Objects;

public final class LeaveAllowanceJpaToDomainMapper {
    public static final String LEAVE_ALLOWANCE_JPA_NOT_NULL = "Leave allowance JPA entity cannot be null";

    private LeaveAllowanceJpaToDomainMapper() {
    }

    public static LeaveAllowance map(LeaveAllowanceJpa leaveAllowanceJpa) {
        Objects.requireNonNull(leaveAllowanceJpa, LEAVE_ALLOWANCE_JPA_NOT_NULL);

        Identity<LeaveAllowance> id = Identity.of(leaveAllowanceJpa.getId());
        return LeaveAllowance.reconstitute(
                id,
                leaveAllowanceJpa.getStaffMemberId(),
                new FullName(
                        leaveAllowanceJpa.getFirstName(),
                        leaveAllowanceJpa.getSurname()
                ),
                leaveAllowanceJpa.getManagerId(),
                new BusinessYear(
                        leaveAllowanceJpa.getBusinessYearStart(),
                        leaveAllowanceJpa.getBusinessYearEnd()
                ),
                new LeaveDays(leaveAllowanceJpa.getBaseEntitlement()),
                new LeaveDays(leaveAllowanceJpa.getCarriedOverDays()),
                new LeaveDays(leaveAllowanceJpa.getRemainingLeaveDays())
        );
    }
}
