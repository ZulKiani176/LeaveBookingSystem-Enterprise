package uk.ac.staffs.leavebooking.leave.domain;

import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveRequestException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotNull;

public final class WorkingDayCalculator {
    public static final String PERIOD_NOT_NULL = "Leave period cannot be null";
    public static final String PORTION_NOT_NULL = "Leave day portion cannot be null";
    public static final String HOLIDAYS_NOT_NULL = "Public holiday dates cannot be null";
    public static final String HALF_DAY_ANNUAL_ONLY = "Half-day leave is available for annual leave only";
    public static final String HALF_DAY_SINGLE_DATE = "Half-day leave must start and end on the same date";
    public static final String HALF_DAY_WORKING_DATE = "Half-day leave must be on a working day";
    public static final String ANNUAL_REQUIRES_WORKING_DAY =
            "Annual leave must include at least one working day";

    public LeaveDays calculate(
            LeaveType leaveType,
            LeavePeriod leavePeriod,
            LeaveDayPortion dayPortion,
            Set<LocalDate> publicHolidays
    ) {
        LeaveType type = argumentNotNull(leaveType, LeaveRequest.LEAVE_TYPE_NOT_NULL);
        LeavePeriod period = argumentNotNull(leavePeriod, PERIOD_NOT_NULL);
        LeaveDayPortion portion = argumentNotNull(dayPortion, PORTION_NOT_NULL);
        Set<LocalDate> holidays = Set.copyOf(argumentNotNull(publicHolidays, HOLIDAYS_NOT_NULL));

        if (type == LeaveType.SICK) {
            if (portion.isHalfDay()) {
                throw new InvalidLeaveRequestException(HALF_DAY_ANNUAL_ONLY);
            }
            return LeaveDays.zero();
        }

        if (portion.isHalfDay()) {
            if (!period.startDate().equals(period.endDate())) {
                throw new InvalidLeaveRequestException(HALF_DAY_SINGLE_DATE);
            }
            if (!isWorkingDay(period.startDate(), holidays)) {
                throw new InvalidLeaveRequestException(HALF_DAY_WORKING_DATE);
            }
            return LeaveDays.of("0.5");
        }

        long workingDays = period.startDate().datesUntil(period.endDate().plusDays(1))
                .filter(date -> isWorkingDay(date, holidays))
                .count();
        if (workingDays == 0) {
            throw new InvalidLeaveRequestException(ANNUAL_REQUIRES_WORKING_DAY);
        }
        return LeaveDays.of(Math.toIntExact(workingDays));
    }

    private boolean isWorkingDay(LocalDate date, Set<LocalDate> publicHolidays) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY
                && date.getDayOfWeek() != DayOfWeek.SUNDAY
                && !publicHolidays.contains(date);
    }
}
