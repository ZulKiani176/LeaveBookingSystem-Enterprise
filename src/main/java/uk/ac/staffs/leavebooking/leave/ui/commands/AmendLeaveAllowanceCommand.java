package uk.ac.staffs.leavebooking.leave.ui.commands;

import java.time.LocalDate;
import java.math.BigDecimal;

public record AmendLeaveAllowanceCommand(
        String staffMemberId,
        LocalDate businessYearStart,
        LocalDate businessYearEnd,
        BigDecimal newEntitlement
) {
    public AmendLeaveAllowanceCommand(
            String staffMemberId,
            LocalDate businessYearStart,
            LocalDate businessYearEnd,
            int newEntitlement
    ) {
        this(staffMemberId, businessYearStart, businessYearEnd, BigDecimal.valueOf(newEntitlement));
    }
}
