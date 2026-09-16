package uk.ac.staffs.leavebooking.leave.application.exceptions;

import java.time.LocalDate;

public class PublicHolidayAlreadyExistsException extends RuntimeException {
    public PublicHolidayAlreadyExistsException(LocalDate date) {
        super("A public holiday already exists for " + date);
    }
}
