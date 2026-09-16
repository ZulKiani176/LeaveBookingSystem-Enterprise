package uk.ac.staffs.leavebooking.common.events.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotNull;

final class IntegrationEventAssertions {
    private IntegrationEventAssertions() {
    }

    static void validPeriod(LocalDate startDate, LocalDate endDate) {
        LocalDate start = argumentNotNull(startDate, "Leave start date cannot be null");
        LocalDate end = argumentNotNull(endDate, "Leave end date cannot be null");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException(
                    "Leave end date cannot be before the start date"
            );
        }
    }

    static BigDecimal nonNegativeDays(BigDecimal days) {
        BigDecimal value = argumentNotNull(days, "Charged leave days cannot be null");
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Charged leave days cannot be negative");
        }
        return value.stripTrailingZeros();
    }

    static BigDecimal inclusiveCalendarDays(LocalDate startDate, LocalDate endDate) {
        validPeriod(startDate, endDate);
        return BigDecimal.valueOf(ChronoUnit.DAYS.between(startDate, endDate) + 1);
    }
}
