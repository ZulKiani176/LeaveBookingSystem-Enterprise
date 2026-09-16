package uk.ac.staffs.leavebooking.leave.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Business-year dates")
class BusinessYearTests {
    private static final LocalDate YEAR_START = LocalDate.of(2026, 4, 1);
    private static final LocalDate YEAR_END = LocalDate.of(2027, 3, 31);

    @Test
    @DisplayName("A valid business year is created successfully")
    void validBusinessYearIsCreatedSuccessfully() {
        BusinessYear businessYear = new BusinessYear(YEAR_START, YEAR_END);

        assertEquals(YEAR_START, businessYear.startDate());
        assertEquals(YEAR_END, businessYear.endDate());
    }

    @Test
    @DisplayName("A same-day business year is structurally valid")
    void sameDayBusinessYearIsValid() {
        assertDoesNotThrow(() -> new BusinessYear(YEAR_START, YEAR_START));
    }

    @Test
    @DisplayName("A null business-year start date is rejected")
    void nullStartDateIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                new BusinessYear(null, YEAR_END)
        );

        assertEquals(BusinessYear.START_DATE_NOT_NULL, exception.getMessage());
    }

    @Test
    @DisplayName("A null business-year end date is rejected")
    void nullEndDateIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                new BusinessYear(YEAR_START, null)
        );

        assertEquals(BusinessYear.END_DATE_NOT_NULL, exception.getMessage());
    }

    @Test
    @DisplayName("A business-year end before its start is rejected")
    void endDateBeforeStartDateIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                new BusinessYear(YEAR_END, YEAR_START)
        );

        assertEquals(BusinessYear.END_DATE_NOT_BEFORE_START, exception.getMessage());
    }

    @Test
    @DisplayName("A leave period fully inside the business year is contained")
    void leavePeriodFullyInsideIsContained() {
        BusinessYear businessYear = validBusinessYear();
        LeavePeriod leavePeriod = new LeavePeriod(
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 14)
        );

        assertTrue(businessYear.contains(leavePeriod));
    }

    @Test
    @DisplayName("A leave period beginning on the business-year start is contained")
    void leavePeriodBeginningOnYearStartIsContained() {
        BusinessYear businessYear = validBusinessYear();
        LeavePeriod leavePeriod = new LeavePeriod(YEAR_START, YEAR_START.plusDays(2));

        assertTrue(businessYear.contains(leavePeriod));
    }

    @Test
    @DisplayName("A leave period ending on the business-year end is contained")
    void leavePeriodEndingOnYearEndIsContained() {
        BusinessYear businessYear = validBusinessYear();
        LeavePeriod leavePeriod = new LeavePeriod(YEAR_END.minusDays(2), YEAR_END);

        assertTrue(businessYear.contains(leavePeriod));
    }

    @Test
    @DisplayName("A leave period beginning before the business year is not contained")
    void leavePeriodBeginningBeforeYearIsNotContained() {
        BusinessYear businessYear = validBusinessYear();
        LeavePeriod leavePeriod = new LeavePeriod(YEAR_START.minusDays(1), YEAR_START);

        assertFalse(businessYear.contains(leavePeriod));
    }

    @Test
    @DisplayName("A leave period ending after the business year is not contained")
    void leavePeriodEndingAfterYearIsNotContained() {
        BusinessYear businessYear = validBusinessYear();
        LeavePeriod leavePeriod = new LeavePeriod(YEAR_END, YEAR_END.plusDays(1));

        assertFalse(businessYear.contains(leavePeriod));
    }

    @Test
    @DisplayName("A null leave period cannot be checked for containment")
    void nullLeavePeriodIsRejected() {
        BusinessYear businessYear = validBusinessYear();

        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                businessYear.contains(null)
        );

        assertEquals(BusinessYear.LEAVE_PERIOD_NOT_NULL, exception.getMessage());
    }

    private BusinessYear validBusinessYear() {
        return new BusinessYear(YEAR_START, YEAR_END);
    }
}
