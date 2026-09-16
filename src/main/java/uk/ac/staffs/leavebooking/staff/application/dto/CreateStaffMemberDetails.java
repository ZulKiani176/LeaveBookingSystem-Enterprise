package uk.ac.staffs.leavebooking.staff.application.dto;

import java.time.LocalDate;

public record CreateStaffMemberDetails(
        String firstName,
        String surname,
        String email,
        LocalDate hireDate,
        String department,
        String managerId,
        String jobRole,
        LocalDate roleStartDate,
        String jobLevel,
        String employmentType
) {
}
