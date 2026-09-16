package uk.ac.staffs.leavebooking.leave.application.mappers;

import uk.ac.staffs.leavebooking.leave.domain.LeaveAllowance;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceJpa;

import java.util.Objects;

public final class LeaveAllowanceDomainToJpaMapper {
    public static final String LEAVE_ALLOWANCE_NOT_NULL = "Leave allowance cannot be null";

    private LeaveAllowanceDomainToJpaMapper() {
    }

    public static LeaveAllowanceJpa map(LeaveAllowance leaveAllowance) {
        return map(leaveAllowance, null);
    }

    public static LeaveAllowanceJpa map(LeaveAllowance leaveAllowance, Long version) {
        Objects.requireNonNull(leaveAllowance, LEAVE_ALLOWANCE_NOT_NULL);

        return new LeaveAllowanceJpa(
                leaveAllowance.id().id(),
                leaveAllowance.staffMemberId(),
                leaveAllowance.fullName().firstName(),
                leaveAllowance.fullName().surname(),
                leaveAllowance.managerId(),
                leaveAllowance.businessYear().startDate(),
                leaveAllowance.businessYear().endDate(),
                leaveAllowance.baseEntitlement().value(),
                leaveAllowance.carriedOverDays().value(),
                leaveAllowance.remainingLeaveDays().value(),
                version
        );
    }
}
