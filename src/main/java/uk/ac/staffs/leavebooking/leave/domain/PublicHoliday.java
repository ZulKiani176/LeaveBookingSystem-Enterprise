package uk.ac.staffs.leavebooking.leave.domain;

import uk.ac.staffs.leavebooking.common.ValueObject;

import java.time.LocalDate;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotEmpty;
import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotNull;

public record PublicHoliday(LocalDate date, String name) implements ValueObject {
    public PublicHoliday {
        date = argumentNotNull(date, "Public holiday date cannot be null");
        name = argumentNotEmpty(name, "Public holiday name cannot be empty");
        if (name.length() > 100) {
            throw new IllegalArgumentException("Public holiday name must not exceed 100 characters");
        }
    }
}
