package uk.ac.staffs.leavebooking.staff.ui.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateStaffMemberRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 20, message = "First name must not exceed 20 characters")
        String firstName,

        @NotBlank(message = "Surname is required")
        @Size(max = 20, message = "Surname must not exceed 20 characters")
        String surname,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 120, message = "Email must not exceed 120 characters")
        String email,

        @NotNull(message = "Hire date is required")
        LocalDate hireDate,

        @NotBlank(message = "Department is required")
        @Size(max = 100, message = "Department must not exceed 100 characters")
        String department,

        @NotBlank(message = "Manager identity is required")
        @Size(max = 36, message = "Manager identity must not exceed 36 characters")
        String managerId,

        @NotBlank(message = "Job role is required")
        @Size(max = 100, message = "Job role must not exceed 100 characters")
        String jobRole,

        @NotNull(message = "Job role start date is required")
        LocalDate roleStartDate,

        @NotBlank(message = "Job level is required")
        @Size(max = 50, message = "Job level must not exceed 50 characters")
        String jobLevel,

        @NotBlank(message = "Employment type is required")
        @Size(max = 50, message = "Employment type must not exceed 50 characters")
        String employmentType
) {
}
