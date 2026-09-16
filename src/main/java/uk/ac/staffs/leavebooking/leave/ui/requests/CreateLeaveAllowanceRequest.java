package uk.ac.staffs.leavebooking.leave.ui.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.math.BigDecimal;

public record CreateLeaveAllowanceRequest(
        @NotNull(message = "Business-year start date is required")
        LocalDate businessYearStart,

        @NotNull(message = "Business-year end date is required")
        LocalDate businessYearEnd,

        @NotNull(message = "Annual entitlement is required")
        @DecimalMin(value = "0.0", message = "Annual entitlement cannot be negative")
        BigDecimal annualEntitlement
) {
}
