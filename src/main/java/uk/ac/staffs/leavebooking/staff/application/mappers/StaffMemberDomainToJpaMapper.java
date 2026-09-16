package uk.ac.staffs.leavebooking.staff.application.mappers;

import uk.ac.staffs.leavebooking.staff.domain.StaffMember;
import uk.ac.staffs.leavebooking.staff.infrastructure.entities.StaffMemberJpa;

import java.util.Objects;

public final class StaffMemberDomainToJpaMapper {
    public static final String STAFF_MEMBER_NOT_NULL = "Staff member cannot be null";

    private StaffMemberDomainToJpaMapper() {
    }

    public static StaffMemberJpa map(StaffMember staffMember) {
        Objects.requireNonNull(staffMember, STAFF_MEMBER_NOT_NULL);

        return new StaffMemberJpa(
                staffMember.id().id(),
                staffMember.fullName().firstName(),
                staffMember.fullName().surname(),
                staffMember.email(),
                staffMember.hireDate(),
                staffMember.department(),
                staffMember.managerId(),
                staffMember.jobRole(),
                staffMember.roleStartDate(),
                staffMember.jobLevel(),
                staffMember.employmentType(),
                staffMember.employmentStatus()
        );
    }
}
