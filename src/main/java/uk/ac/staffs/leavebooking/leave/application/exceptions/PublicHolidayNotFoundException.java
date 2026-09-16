package uk.ac.staffs.leavebooking.leave.application.exceptions;

import java.time.LocalDate;

public class PublicHolidayNotFoundException extends RuntimeException {
    public PublicHolidayNotFoundException(LocalDate date) {
        super("No public holiday exists for " + date);
    }
}
