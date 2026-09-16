package uk.ac.staffs.leavebooking.leave.ui.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AmendLeaveAllowanceRequest(
        @NotNull(message = "New entitlement is required")
        @DecimalMin(value = "0.0", message = "New entitlement cannot be negative")
        BigDecimal newEntitlement
) {
}
