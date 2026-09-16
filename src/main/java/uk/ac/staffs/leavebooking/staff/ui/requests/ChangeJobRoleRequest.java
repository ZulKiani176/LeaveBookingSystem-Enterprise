package uk.ac.staffs.leavebooking.staff.ui.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ChangeJobRoleRequest(
        @NotBlank(message = "Job role is required")
        @Size(max = 100, message = "Job role must not exceed 100 characters")
        String jobRole,

        @NotNull(message = "Job role start date is required")
        LocalDate roleStartDate
) {
}
