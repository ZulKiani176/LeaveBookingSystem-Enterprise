package uk.ac.staffs.leavebooking.leave.application.exceptions;

import java.time.LocalDate;

public class LeaveAllowanceAlreadyExistsException extends RuntimeException {
    public LeaveAllowanceAlreadyExistsException(
            String staffMemberId,
            LocalDate businessYearStart,
            LocalDate businessYearEnd
    ) {
        super("Leave allowance already exists for staff member " + staffMemberId
                + " and business year " + businessYearStart + " to " + businessYearEnd);
    }
}
