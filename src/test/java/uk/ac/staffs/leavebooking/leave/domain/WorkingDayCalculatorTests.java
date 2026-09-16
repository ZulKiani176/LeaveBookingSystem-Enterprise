package uk.ac.staffs.leavebooking.leave.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveRequestException;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Counting working days")
class WorkingDayCalculatorTests {
    private final WorkingDayCalculator calculator = new WorkingDayCalculator();

    @Test
    @DisplayName("Annual leave excludes weekends")
    void annualLeaveExcludesWeekends() {
        LeaveDays result = calculator.calculate(
                LeaveType.ANNUAL,
                period("2026-09-18", "2026-09-21"),
                LeaveDayPortion.FULL_DAY,
                Set.of()
        );

        assertEquals(LeaveDays.of("2.0"), result);
    }

    @Test
    @DisplayName("Annual leave excludes configured public holidays")
    void annualLeaveExcludesPublicHolidays() {
        LeaveDays result = calculator.calculate(
                LeaveType.ANNUAL,
                period("2026-08-24", "2026-08-28"),
                LeaveDayPortion.FULL_DAY,
                Set.of(LocalDate.parse("2026-08-26"))
        );

        assertEquals(LeaveDays.of("4.0"), result);
    }

    @Test
    @DisplayName("Morning and afternoon annual sessions each charge half a day")
    void halfDayAnnualLeaveChargesHalfDay() {
        LeavePeriod date = period("2026-08-24", "2026-08-24");

        assertEquals(LeaveDays.of("0.5"), calculator.calculate(
                LeaveType.ANNUAL, date, LeaveDayPortion.MORNING, Set.of()
        ));
        assertEquals(LeaveDays.of("0.5"), calculator.calculate(
                LeaveType.ANNUAL, date, LeaveDayPortion.AFTERNOON, Set.of()
        ));
    }

    @Test
    @DisplayName("A half day cannot span multiple dates")
    void halfDayMustUseOneDate() {
        assertThrows(InvalidLeaveRequestException.class, () -> calculator.calculate(
                LeaveType.ANNUAL,
                period("2026-08-24", "2026-08-25"),
                LeaveDayPortion.MORNING,
                Set.of()
        ));
    }

    @Test
    @DisplayName("A half day cannot be booked on a weekend or public holiday")
    void halfDayRequiresWorkingDate() {
        assertThrows(InvalidLeaveRequestException.class, () -> calculator.calculate(
                LeaveType.ANNUAL,
                period("2026-08-22", "2026-08-22"),
                LeaveDayPortion.MORNING,
                Set.of()
        ));
        assertThrows(InvalidLeaveRequestException.class, () -> calculator.calculate(
                LeaveType.ANNUAL,
                period("2026-08-24", "2026-08-24"),
                LeaveDayPortion.AFTERNOON,
                Set.of(LocalDate.parse("2026-08-24"))
        ));
    }

    @Test
    @DisplayName("Annual leave containing no working day is rejected")
    void annualLeaveRequiresWorkingDay() {
        InvalidLeaveRequestException exception = assertThrows(
                InvalidLeaveRequestException.class,
                () -> calculator.calculate(
                        LeaveType.ANNUAL,
                        period("2026-08-22", "2026-08-23"),
                        LeaveDayPortion.FULL_DAY,
                        Set.of()
                )
        );

        assertEquals(WorkingDayCalculator.ANNUAL_REQUIRES_WORKING_DAY, exception.getMessage());
    }

    @Test
    @DisplayName("Sickness records consume no annual allowance")
    void sicknessHasZeroAnnualCharge() {
        assertEquals(LeaveDays.zero(), calculator.calculate(
                LeaveType.SICK,
                period("2026-08-22", "2026-08-24"),
                LeaveDayPortion.FULL_DAY,
                Set.of()
        ));
    }

    @Test
    @DisplayName("Sickness cannot use an annual half-day session")
    void sicknessCannotUseHalfDayPortion() {
        assertThrows(InvalidLeaveRequestException.class, () -> calculator.calculate(
                LeaveType.SICK,
                period("2026-08-24", "2026-08-24"),
                LeaveDayPortion.MORNING,
                Set.of()
        ));
    }

    private LeavePeriod period(String start, String end) {
        return new LeavePeriod(LocalDate.parse(start), LocalDate.parse(end));
    }
}
