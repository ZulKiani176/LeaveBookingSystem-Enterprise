package uk.ac.staffs.leavebooking.leave.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Leave dates")
class LeavePeriodTests {
    @Test
    @DisplayName("A leave period requires a start date")
    void nullStartDateThrowsExpectedException() {
        LocalDate endDate = LocalDate.of(2026, 9, 25);

        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                new LeavePeriod(null, endDate)
        );

        assertEquals(LeavePeriod.START_DATE_NOT_NULL, exception.getMessage());
    }

    @Test
    @DisplayName("A leave period requires an end date")
    void nullEndDateThrowsExpectedException() {
        LocalDate startDate = LocalDate.of(2026, 9, 21);

        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                new LeavePeriod(startDate, null)
        );

        assertEquals(LeavePeriod.END_DATE_NOT_NULL, exception.getMessage());
    }

    @Test
    @DisplayName("A leave period end date cannot be before its start date")
    void endDateBeforeStartDateThrowsExpectedException() {
        LocalDate startDate = LocalDate.of(2026, 9, 25);
        LocalDate endDate = LocalDate.of(2026, 9, 21);

        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                new LeavePeriod(startDate, endDate)
        );

        assertEquals(LeavePeriod.END_DATE_NOT_BEFORE_START, exception.getMessage());
    }

    @Test
    @DisplayName("Leave periods with the same dates are equal")
    void leavePeriodsWithSameDatesAreEqual() {
        LeavePeriod leavePeriod = new LeavePeriod(
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 25)
        );
        LeavePeriod matchingLeavePeriod = new LeavePeriod(
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 25)
        );

        assertEquals(leavePeriod, matchingLeavePeriod);
    }

    @Test
    @DisplayName("Leave periods with different dates are not equal")
    void leavePeriodsWithDifferentDatesAreNotEqual() {
        LeavePeriod leavePeriod = new LeavePeriod(
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 25)
        );
        LeavePeriod differentLeavePeriod = new LeavePeriod(
                LocalDate.of(2026, 9, 22),
                LocalDate.of(2026, 9, 25)
        );

        assertNotEquals(leavePeriod, differentLeavePeriod);
    }

    @Test
    @DisplayName("A one-day leave period contains one calendar day")
    void sameDayPeriodContainsOneCalendarDay() {
        LeavePeriod period = new LeavePeriod(
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 14)
        );

        int calendarDays = period.calendarDays();

        assertAll(
                () -> assertEquals(LocalDate.of(2026, 9, 14), period.startDate()),
                () -> assertEquals(period.startDate(), period.endDate()),
                () -> assertEquals(1, calendarDays)
        );
    }

    @Test
    @DisplayName("A Monday-to-Friday leave period contains five calendar days")
    void mondayToFridayContainsFiveCalendarDays() {
        LeavePeriod period = new LeavePeriod(
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 18)
        );

        int calendarDays = period.calendarDays();

        assertAll(
                () -> assertEquals(LocalDate.of(2026, 9, 14), period.startDate()),
                () -> assertEquals(LocalDate.of(2026, 9, 18), period.endDate()),
                () -> assertEquals(5, calendarDays)
        );
    }

    @Test
    @DisplayName("Calendar-day calculation currently includes weekend dates")
    void calendarDayCalculationIncludesWeekendDates() {
        LeavePeriod period = new LeavePeriod(
                LocalDate.of(2026, 9, 18),
                LocalDate.of(2026, 9, 21)
        );

        int calendarDays = period.calendarDays();

        assertEquals(4, calendarDays);
    }
}
