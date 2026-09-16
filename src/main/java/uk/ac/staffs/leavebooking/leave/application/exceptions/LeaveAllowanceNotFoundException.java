package uk.ac.staffs.leavebooking.leave.application.exceptions;

import java.time.LocalDate;

public class LeaveAllowanceNotFoundException extends RuntimeException {
    public LeaveAllowanceNotFoundException(
            String staffMemberId,
            LocalDate businessYearStart,
            LocalDate businessYearEnd
    ) {
        this(
                "Leave allowance not found for staff member " + staffMemberId
                        + " and business year " + businessYearStart + " to " + businessYearEnd
        );
    }

    private LeaveAllowanceNotFoundException(String message) {
        super(message);
    }

    public static LeaveAllowanceNotFoundException forLeavePeriod(
            String staffMemberId,
            LocalDate leaveStart,
            LocalDate leaveEnd
    ) {
        return new LeaveAllowanceNotFoundException(
                "No leave allowance for staff member " + staffMemberId
                        + " contains leave period " + leaveStart + " to " + leaveEnd
        );
    }
}
