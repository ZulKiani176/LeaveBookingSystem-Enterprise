package uk.ac.staffs.leavebooking.leave.ui.commands;

import java.time.LocalDate;
import java.math.BigDecimal;

public record CreateLeaveAllowanceCommand(
        String staffMemberId,
        LocalDate businessYearStart,
        LocalDate businessYearEnd,
        BigDecimal baseEntitlement
) {
    public CreateLeaveAllowanceCommand(
            String staffMemberId,
            LocalDate businessYearStart,
            LocalDate businessYearEnd,
            int baseEntitlement
    ) {
        this(staffMemberId, businessYearStart, businessYearEnd, BigDecimal.valueOf(baseEntitlement));
    }
}
