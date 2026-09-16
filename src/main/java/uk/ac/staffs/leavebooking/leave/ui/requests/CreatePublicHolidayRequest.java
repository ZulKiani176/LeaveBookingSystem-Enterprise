package uk.ac.staffs.leavebooking.leave.ui.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreatePublicHolidayRequest(
        @NotNull(message = "Public holiday date is required")
        LocalDate date,
        @NotBlank(message = "Public holiday name is required")
        @Size(max = 100, message = "Public holiday name must not exceed 100 characters")
        String name
) {
}
