package uk.ac.staffs.leavebooking.staff.application.mappers;

import uk.ac.staffs.leavebooking.staff.application.dto.StaffMemberDTO;
import uk.ac.staffs.leavebooking.staff.infrastructure.entities.StaffMemberJpa;

import java.util.Objects;

public final class StaffMemberJpaToDTOMapper {
    public static final String STAFF_MEMBER_JPA_NOT_NULL = "Staff member JPA entity cannot be null";

    private StaffMemberJpaToDTOMapper() {
    }

    public static StaffMemberDTO map(StaffMemberJpa staffMemberJpa) {
        Objects.requireNonNull(staffMemberJpa, STAFF_MEMBER_JPA_NOT_NULL);

        return new StaffMemberDTO(
                staffMemberJpa.getId(),
                staffMemberJpa.getFirstName(),
                staffMemberJpa.getSurname(),
                staffMemberJpa.getEmail(),
                staffMemberJpa.getHireDate(),
                staffMemberJpa.getDepartment(),
                staffMemberJpa.getManagerId(),
                staffMemberJpa.getJobRole(),
                staffMemberJpa.getRoleStartDate(),
                staffMemberJpa.getJobLevel(),
                staffMemberJpa.getEmploymentType(),
                staffMemberJpa.getEmploymentStatus()
        );
    }
}
