package uk.ac.staffs.leavebooking.leave.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Overlapping leave")
class LeaveOverlapPolicyTests {
    private static final LeavePeriod DATE = period("2026-08-24", "2026-08-24");

    @Test
    @DisplayName("The same half-day session conflicts")
    void sameHalfDaySessionConflicts() {
        assertTrue(LeaveOverlapPolicy.conflicts(
                DATE, LeaveDayPortion.MORNING, DATE, LeaveDayPortion.MORNING
        ));
        assertTrue(LeaveOverlapPolicy.conflicts(
                DATE, LeaveDayPortion.AFTERNOON, DATE, LeaveDayPortion.AFTERNOON
        ));
    }

    @Test
    @DisplayName("Complementary morning and afternoon sessions are allowed")
    void complementaryHalfDaysDoNotConflict() {
        assertFalse(LeaveOverlapPolicy.conflicts(
                DATE, LeaveDayPortion.MORNING, DATE, LeaveDayPortion.AFTERNOON
        ));
    }

    @Test
    @DisplayName("A full day conflicts with either half-day session")
    void fullDayConflictsWithHalfDay() {
        assertTrue(LeaveOverlapPolicy.conflicts(
                DATE, LeaveDayPortion.FULL_DAY, DATE, LeaveDayPortion.MORNING
        ));
        assertTrue(LeaveOverlapPolicy.conflicts(
                DATE, LeaveDayPortion.AFTERNOON, DATE, LeaveDayPortion.FULL_DAY
        ));
    }

    @Test
    @DisplayName("Date ranges conflict on inclusive boundaries")
    void dateRangesConflictOnBoundaries() {
        assertTrue(LeaveOverlapPolicy.conflicts(
                period("2026-08-20", "2026-08-24"), LeaveDayPortion.FULL_DAY,
                period("2026-08-24", "2026-08-28"), LeaveDayPortion.FULL_DAY
        ));
    }

    @Test
    @DisplayName("Separate date ranges do not conflict")
    void separateRangesDoNotConflict() {
        assertFalse(LeaveOverlapPolicy.conflicts(
                period("2026-08-20", "2026-08-23"), LeaveDayPortion.FULL_DAY,
                period("2026-08-24", "2026-08-28"), LeaveDayPortion.FULL_DAY
        ));
    }

    private static LeavePeriod period(String start, String end) {
        return new LeavePeriod(LocalDate.parse(start), LocalDate.parse(end));
    }
}
