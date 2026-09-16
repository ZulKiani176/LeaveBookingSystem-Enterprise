package uk.ac.staffs.leavebooking.staff.application.mappers;

import uk.ac.staffs.leavebooking.common.FullName;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.staff.domain.StaffMember;
import uk.ac.staffs.leavebooking.staff.infrastructure.entities.StaffMemberJpa;

import java.util.Objects;

public final class StaffMemberJpaToDomainMapper {
    public static final String STAFF_MEMBER_JPA_NOT_NULL = "Staff member JPA entity cannot be null";

    private StaffMemberJpaToDomainMapper() {
    }

    public static StaffMember map(StaffMemberJpa staffMemberJpa) {
        Objects.requireNonNull(staffMemberJpa, STAFF_MEMBER_JPA_NOT_NULL);

        return StaffMember.reconstitute(
                Identity.of(staffMemberJpa.getId()),
                new FullName(staffMemberJpa.getFirstName(), staffMemberJpa.getSurname()),
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
