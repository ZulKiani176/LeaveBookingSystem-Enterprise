package uk.ac.staffs.leavebooking.staff.application.mappers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.ac.staffs.leavebooking.common.FullName;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.staff.application.dto.StaffMemberDTO;
import uk.ac.staffs.leavebooking.staff.domain.EmploymentStatus;
import uk.ac.staffs.leavebooking.staff.domain.StaffMember;
import uk.ac.staffs.leavebooking.staff.infrastructure.entities.StaffMemberJpa;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Staff Member Mapper")
class StaffMemberMapperTests {
    private static final LocalDate HIRE_DATE = LocalDate.of(2024, 4, 1);
    private static final LocalDate ROLE_START_DATE = LocalDate.of(2025, 8, 1);

    @Test
    @DisplayName("A complete staff aggregate survives a domain-JPA-domain round-trip")
    void completeStaffMemberRoundTripPreservesEveryField() {
        StaffMember original = staffMember(EmploymentStatus.ON_LEAVE);

        StaffMemberJpa persisted = StaffMemberDomainToJpaMapper.map(original);
        StaffMember restored = StaffMemberJpaToDomainMapper.map(persisted);

        assertEquals(original.id(), restored.id());
        assertEquals(original.fullName(), restored.fullName());
        assertEquals(original.email(), restored.email());
        assertEquals(original.hireDate(), restored.hireDate());
        assertEquals(original.department(), restored.department());
        assertEquals(original.managerId(), restored.managerId());
        assertEquals(original.jobRole(), restored.jobRole());
        assertEquals(original.roleStartDate(), restored.roleStartDate());
        assertEquals(original.jobLevel(), restored.jobLevel());
        assertEquals(original.employmentType(), restored.employmentType());
        assertEquals(original.employmentStatus(), restored.employmentStatus());
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @EnumSource(EmploymentStatus.class)
    @DisplayName("JPA-to-domain mapping reconstructs every employment status")
    void jpaToDomainMapperPreservesEmploymentStatus(EmploymentStatus status) {
        StaffMember restored = StaffMemberJpaToDomainMapper.map(staffMemberJpa(status));

        assertEquals(status, restored.employmentStatus());
    }

    @Test
    @DisplayName("JPA-to-DTO mapping exposes every staff reporting field")
    void jpaToDtoMapperMapsEveryField() {
        StaffMemberDTO dto = StaffMemberJpaToDTOMapper.map(
                staffMemberJpa(EmploymentStatus.TERMINATED)
        );

        assertEquals("staff-1", dto.id());
        assertEquals("Ada", dto.firstName());
        assertEquals("Lovelace", dto.surname());
        assertEquals("ada@example.com", dto.email());
        assertEquals(HIRE_DATE, dto.hireDate());
        assertEquals("Engineering", dto.department());
        assertEquals("manager-1", dto.managerId());
        assertEquals("Team Leader", dto.jobRole());
        assertEquals(ROLE_START_DATE, dto.roleStartDate());
        assertEquals("Level 3", dto.jobLevel());
        assertEquals("Permanent", dto.employmentType());
        assertEquals(EmploymentStatus.TERMINATED, dto.employmentStatus());
    }

    @Test
    @DisplayName("Domain-to-JPA mapping rejects null input")
    void domainToJpaMapperRejectsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> StaffMemberDomainToJpaMapper.map(null)
        );

        assertEquals(StaffMemberDomainToJpaMapper.STAFF_MEMBER_NOT_NULL, exception.getMessage());
    }

    @Test
    @DisplayName("JPA-to-domain mapping rejects null input")
    void jpaToDomainMapperRejectsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> StaffMemberJpaToDomainMapper.map(null)
        );

        assertEquals(StaffMemberJpaToDomainMapper.STAFF_MEMBER_JPA_NOT_NULL, exception.getMessage());
    }

    @Test
    @DisplayName("JPA-to-DTO mapping rejects null input")
    void jpaToDtoMapperRejectsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> StaffMemberJpaToDTOMapper.map(null)
        );

        assertEquals(StaffMemberJpaToDTOMapper.STAFF_MEMBER_JPA_NOT_NULL, exception.getMessage());
    }

    private StaffMember staffMember(EmploymentStatus status) {
        return StaffMember.reconstitute(
                Identity.of("staff-1"),
                new FullName("Ada", "Lovelace"),
                "ada@example.com",
                HIRE_DATE,
                "Engineering",
                "manager-1",
                "Team Leader",
                ROLE_START_DATE,
                "Level 3",
                "Permanent",
                status
        );
    }

    private StaffMemberJpa staffMemberJpa(EmploymentStatus status) {
        return new StaffMemberJpa(
                "staff-1",
                "Ada",
                "Lovelace",
                "ada@example.com",
                HIRE_DATE,
                "Engineering",
                "manager-1",
                "Team Leader",
                ROLE_START_DATE,
                "Level 3",
                "Permanent",
                status
        );
    }
}
