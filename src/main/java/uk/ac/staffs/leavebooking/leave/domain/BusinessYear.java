package uk.ac.staffs.leavebooking.leave.domain;

import uk.ac.staffs.leavebooking.common.ValueObject;

import java.time.LocalDate;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotNull;

public record BusinessYear(
        LocalDate startDate,
        LocalDate endDate
) implements ValueObject {
    public static final String START_DATE_NOT_NULL = "Business year start date cannot be null";
    public static final String END_DATE_NOT_NULL = "Business year end date cannot be null";
    public static final String END_DATE_NOT_BEFORE_START =
            "Business year end date cannot be before the start date";
    public static final String LEAVE_PERIOD_NOT_NULL = "Leave period cannot be null";

    public BusinessYear {
        startDate = argumentNotNull(startDate, START_DATE_NOT_NULL);
        endDate = argumentNotNull(endDate, END_DATE_NOT_NULL);

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(END_DATE_NOT_BEFORE_START);
        }
    }

    public boolean contains(LeavePeriod leavePeriod) {
        leavePeriod = argumentNotNull(leavePeriod, LEAVE_PERIOD_NOT_NULL);
        return !leavePeriod.startDate().isBefore(startDate)
                && !leavePeriod.endDate().isAfter(endDate);
    }
}
