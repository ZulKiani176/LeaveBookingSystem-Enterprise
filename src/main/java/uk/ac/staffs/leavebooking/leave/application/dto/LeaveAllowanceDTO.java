package uk.ac.staffs.leavebooking.leave.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.math.BigDecimal;

public record LeaveAllowanceDTO(
        String id,
        String staffMemberId,
        String firstName,
        String surname,
        String managerId,
        LocalDate businessYearStart,
        LocalDate businessYearEnd,
        BigDecimal baseEntitlement,
        BigDecimal carriedOverDays,
        BigDecimal totalEntitlement,
        BigDecimal remainingDays,
        BigDecimal usedDays
) {
    public LeaveAllowanceDTO(
            String id,
            String staffMemberId,
            String firstName,
            String surname,
            String managerId,
            LocalDate businessYearStart,
            LocalDate businessYearEnd,
            int annualEntitlement,
            int remainingDays,
            int usedDays
    ) {
        this(
                id, staffMemberId, firstName, surname, managerId,
                businessYearStart, businessYearEnd,
                BigDecimal.valueOf(annualEntitlement), BigDecimal.ZERO,
                BigDecimal.valueOf(annualEntitlement),
                BigDecimal.valueOf(remainingDays), BigDecimal.valueOf(usedDays)
        );
    }

    @Deprecated(forRemoval = false)
    @JsonIgnore
    public int annualEntitlement() {
        return totalEntitlement.intValueExact();
    }

    @JsonProperty("annualEntitlement")
    public BigDecimal legacyAnnualEntitlement() {
        return totalEntitlement;
    }
}
