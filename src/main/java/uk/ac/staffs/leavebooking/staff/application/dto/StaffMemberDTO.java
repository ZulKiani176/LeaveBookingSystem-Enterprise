package uk.ac.staffs.leavebooking.staff.application.dto;

import uk.ac.staffs.leavebooking.staff.domain.EmploymentStatus;

import java.time.LocalDate;

public record StaffMemberDTO(
        String id,
        String firstName,
        String surname,
        String email,
        LocalDate hireDate,
        String department,
        String managerId,
        String jobRole,
        LocalDate roleStartDate,
        String jobLevel,
        String employmentType,
        EmploymentStatus employmentStatus
) {
}
