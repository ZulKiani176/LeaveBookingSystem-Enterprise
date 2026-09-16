package uk.ac.staffs.leavebooking.leave.domain;

import uk.ac.staffs.leavebooking.common.ValueObject;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotNull;

public record LeavePeriod(
        LocalDate startDate,
        LocalDate endDate
) implements ValueObject {
    public static final String START_DATE_NOT_NULL = "Leave start date cannot be null";
    public static final String END_DATE_NOT_NULL = "Leave end date cannot be null";
    public static final String END_DATE_NOT_BEFORE_START = "Leave end date cannot be before the start date";

    public LeavePeriod {
        startDate = argumentNotNull(startDate, START_DATE_NOT_NULL);
        endDate = argumentNotNull(endDate, END_DATE_NOT_NULL);

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(END_DATE_NOT_BEFORE_START);
        }
    }

    public int calendarDays() {
        return Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate) + 1);
    }
}
