package uk.ac.staffs.leavebooking.leave.application.dto;

import java.time.LocalDate;
import java.math.BigDecimal;

public record LeaveUsageSummaryDTO(
        LocalDate businessYearStart,
        LocalDate businessYearEnd,
        int staffCount,
        BigDecimal totalEntitlement,
        BigDecimal totalRemainingDays,
        BigDecimal totalUsedDays
) {
    public LeaveUsageSummaryDTO(
            LocalDate businessYearStart,
            LocalDate businessYearEnd,
            int staffCount,
            int totalEntitlement,
            int totalRemainingDays,
            int totalUsedDays
    ) {
        this(
                businessYearStart, businessYearEnd, staffCount,
                BigDecimal.valueOf(totalEntitlement),
                BigDecimal.valueOf(totalRemainingDays),
                BigDecimal.valueOf(totalUsedDays)
        );
    }
}
