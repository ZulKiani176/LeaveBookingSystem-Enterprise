package uk.ac.staffs.leavebooking.leave.ui.commands;

import java.time.LocalDate;

public record CarryOverLeaveAllowanceCommand(
        String staffMemberId,
        LocalDate sourceYearStart,
        LocalDate sourceYearEnd,
        LocalDate targetYearStart,
        LocalDate targetYearEnd
) {
}
