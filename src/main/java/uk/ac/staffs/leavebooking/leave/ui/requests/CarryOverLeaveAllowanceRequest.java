package uk.ac.staffs.leavebooking.leave.ui.requests;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CarryOverLeaveAllowanceRequest(
        @NotNull LocalDate sourceYearStart,
        @NotNull LocalDate sourceYearEnd,
        @NotNull LocalDate targetYearStart,
        @NotNull LocalDate targetYearEnd
) {
}
