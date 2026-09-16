package uk.ac.staffs.leavebooking.leave.application.dto;

import java.time.LocalDate;

public record PublicHolidayDTO(LocalDate date, String name) {
}
