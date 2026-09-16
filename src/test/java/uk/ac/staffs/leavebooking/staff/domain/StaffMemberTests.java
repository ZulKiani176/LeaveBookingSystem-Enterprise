package uk.ac.staffs.leavebooking.staff.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.staffs.leavebooking.common.FullName;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.staff.domain.exceptions.InvalidStaffMemberException;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Staff Member")
class StaffMemberTests {
    private static final Identity<StaffMember> STAFF_ID = Identity.of("staff-1");
    private static final FullName FULL_NAME = new FullName("Ada", "Lovelace");
    private static final LocalDate HIRE_DATE = LocalDate.of(2024, 4, 1);
    private static final LocalDate ROLE_START_DATE = LocalDate.of(2024, 4, 1);

    @Test
    @DisplayName("A valid staff member retains its complete organisational and placement state")
    void validStaffMemberIsCreated() {
        StaffMember staffMember = validStaffMember();

        assertEquals(STAFF_ID, staffMember.id());
        assertEquals(FULL_NAME, staffMember.fullName());
        assertEquals("ada@example.com", staffMember.email());
        assertEquals(HIRE_DATE, staffMember.hireDate());
        assertEquals("Engineering", staffMember.department());
        assertEquals("manager-1", staffMember.managerId());
        assertEquals("Software Engineer", staffMember.jobRole());
        assertEquals(ROLE_START_DATE, staffMember.roleStartDate());
        assertEquals("Level 2", staffMember.jobLevel());
        assertEquals("Permanent", staffMember.employmentType());
    }

    @Test
    @DisplayName("A new staff member starts in active employment")
    void newStaffMemberStartsActive() {
        StaffMember staffMember = validStaffMember();

        assertEquals(EmploymentStatus.ACTIVE, staffMember.employmentStatus());
    }

    @Test
    @DisplayName("Surrounding whitespace is trimmed from every stored String field")
    void stringFieldsAreTrimmed() {
        StaffMember staffMember = new StaffMember(
                STAFF_ID,
                FULL_NAME,
                "  ada@example.com  ",
                HIRE_DATE,
                "  Engineering  ",
                "  manager-1  ",
                "  Software Engineer  ",
                ROLE_START_DATE,
                "  Level 2  ",
                "  Permanent  "
        );

        assertEquals("ada@example.com", staffMember.email());
        assertEquals("Engineering", staffMember.department());
        assertEquals("manager-1", staffMember.managerId());
        assertEquals("Software Engineer", staffMember.jobRole());
        assertEquals("Level 2", staffMember.jobLevel());
        assertEquals("Permanent", staffMember.employmentType());
    }

    @Test
    @DisplayName("A null full name is rejected")
    void nullFullNameIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> new StaffMember(
                STAFF_ID,
                null,
                "ada@example.com",
                HIRE_DATE,
                "Engineering",
                "manager-1",
                "Software Engineer",
                ROLE_START_DATE,
                "Level 2",
                "Permanent"
        ));

        assertEquals(StaffMember.FULL_NAME_NOT_NULL, exception.getMessage());
    }

    @Test
    @DisplayName("A null hire date is rejected")
    void nullHireDateIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> new StaffMember(
                STAFF_ID,
                FULL_NAME,
                "ada@example.com",
                null,
                "Engineering",
                "manager-1",
                "Software Engineer",
                ROLE_START_DATE,
                "Level 2",
                "Permanent"
        ));

        assertEquals(StaffMember.HIRE_DATE_NOT_NULL, exception.getMessage());
    }

    @Test
    @DisplayName("A null role start date is rejected")
    void nullRoleStartDateIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> new StaffMember(
                STAFF_ID,
                FULL_NAME,
                "ada@example.com",
                HIRE_DATE,
                "Engineering",
                "manager-1",
                "Software Engineer",
                null,
                "Level 2",
                "Permanent"
        ));

        assertEquals(StaffMember.ROLE_START_DATE_NOT_NULL, exception.getMessage());
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @MethodSource("invalidRequiredStrings")
    @DisplayName("Required String fields reject null, empty and blank values")
    void requiredStringFieldsRejectEmptyValues(String field, String invalidValue, String message) {
        Throwable exception = assertThrows(
                IllegalArgumentException.class,
                () -> staffMemberWith(field, invalidValue)
        );

        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("A job role start date before the hire date is rejected")
    void roleStartBeforeHireDateIsRejected() {
        Throwable exception = assertThrows(InvalidStaffMemberException.class, () -> new StaffMember(
                STAFF_ID,
                FULL_NAME,
                "ada@example.com",
                HIRE_DATE,
                "Engineering",
                "manager-1",
                "Software Engineer",
                HIRE_DATE.minusDays(1),
                "Level 2",
                "Permanent"
        ));

        assertEquals(StaffMember.ROLE_START_BEFORE_HIRE, exception.getMessage());
    }

    @Test
    @DisplayName("Changing department stores the trimmed replacement")
    void departmentCanBeChanged() {
        StaffMember staffMember = validStaffMember();

        staffMember.changeDepartment("  Finance  ");

        assertEquals("Finance", staffMember.department());
    }

    @Test
    @DisplayName("A failed department change leaves the original department unchanged")
    void invalidDepartmentChangeDoesNotMutateState() {
        StaffMember staffMember = validStaffMember();

        Throwable exception = assertThrows(
                IllegalArgumentException.class,
                () -> staffMember.changeDepartment("   ")
        );

        assertEquals(StaffMember.DEPARTMENT_NOT_EMPTY, exception.getMessage());
        assertEquals("Engineering", staffMember.department());
    }

    @Test
    @DisplayName("Changing job role updates the trimmed role and its start date together")
    void jobRoleCanBeChanged() {
        StaffMember staffMember = validStaffMember();
        LocalDate newStartDate = LocalDate.of(2026, 8, 1);

        staffMember.changeJobRole("  Team Leader  ", newStartDate);

        assertEquals("Team Leader", staffMember.jobRole());
        assertEquals(newStartDate, staffMember.roleStartDate());
    }

    @Test
    @DisplayName("A blank replacement role leaves the existing placement unchanged")
    void blankJobRoleChangeDoesNotMutateState() {
        StaffMember staffMember = validStaffMember();

        Throwable exception = assertThrows(
                IllegalArgumentException.class,
                () -> staffMember.changeJobRole("   ", LocalDate.of(2026, 8, 1))
        );

        assertEquals(StaffMember.JOB_ROLE_NOT_EMPTY, exception.getMessage());
        assertEquals("Software Engineer", staffMember.jobRole());
        assertEquals(ROLE_START_DATE, staffMember.roleStartDate());
    }

    @Test
    @DisplayName("An invalid replacement role date leaves the existing placement unchanged")
    void invalidJobRoleDateDoesNotMutateState() {
        StaffMember staffMember = validStaffMember();

        Throwable exception = assertThrows(
                InvalidStaffMemberException.class,
                () -> staffMember.changeJobRole("Team Leader", HIRE_DATE.minusDays(1))
        );

        assertEquals(StaffMember.ROLE_START_BEFORE_HIRE, exception.getMessage());
        assertEquals("Software Engineer", staffMember.jobRole());
        assertEquals(ROLE_START_DATE, staffMember.roleStartDate());
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @EnumSource(EmploymentStatus.class)
    @DisplayName("Persistence reconstruction supports every employment status")
    void reconstructionSupportsEveryEmploymentStatus(EmploymentStatus status) {
        StaffMember staffMember = reconstitutedStaffMember(status);

        assertEquals(status, staffMember.employmentStatus());
        assertEquals(STAFF_ID, staffMember.id());
        assertEquals(FULL_NAME, staffMember.fullName());
        assertEquals("ada@example.com", staffMember.email());
    }

    @Test
    @DisplayName("Persistence reconstruction rejects a null employment status")
    void reconstructionRejectsNullEmploymentStatus() {
        Throwable exception = assertThrows(
                IllegalArgumentException.class,
                () -> reconstitutedStaffMember(null)
        );

        assertEquals(StaffMember.EMPLOYMENT_STATUS_NOT_NULL, exception.getMessage());
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @EnumSource(EmploymentStatus.class)
    @DisplayName("External HR creation preserves the supplied identity and employment status")
    void externalHrCreationPreservesIdentityAndStatus(EmploymentStatus status) {
        Identity<StaffMember> externalId = Identity.of("hr-staff-42");

        StaffMember staffMember = StaffMember.createFromExternalHr(
                externalId,
                FULL_NAME,
                "ada@example.com",
                HIRE_DATE,
                "Engineering",
                "manager-1",
                "Software Engineer",
                ROLE_START_DATE,
                "Level 2",
                "Permanent",
                status
        );

        assertEquals(externalId, staffMember.id());
        assertEquals(status, staffMember.employmentStatus());
    }

    @Test
    @DisplayName("Changing personal details stores the validated and trimmed values together")
    void personalDetailsCanBeChanged() {
        StaffMember staffMember = validStaffMember();

        staffMember.changePersonalDetails(
                new FullName("  Grace  ", "  Hopper  "),
                "  grace@example.com  "
        );

        assertEquals(new FullName("Grace", "Hopper"), staffMember.fullName());
        assertEquals("grace@example.com", staffMember.email());
    }

    @Test
    @DisplayName("An invalid personal-details change leaves both existing values unchanged")
    void invalidPersonalDetailsChangeDoesNotMutateState() {
        StaffMember staffMember = validStaffMember();

        Throwable exception = assertThrows(
                IllegalArgumentException.class,
                () -> staffMember.changePersonalDetails(
                        new FullName("Grace", "Hopper"),
                        "   "
                )
        );

        assertEquals(StaffMember.EMAIL_NOT_EMPTY, exception.getMessage());
        assertEquals(FULL_NAME, staffMember.fullName());
        assertEquals("ada@example.com", staffMember.email());
    }

    private StaffMember validStaffMember() {
        return new StaffMember(
                STAFF_ID,
                FULL_NAME,
                "ada@example.com",
                HIRE_DATE,
                "Engineering",
                "manager-1",
                "Software Engineer",
                ROLE_START_DATE,
                "Level 2",
                "Permanent"
        );
    }

    private StaffMember reconstitutedStaffMember(EmploymentStatus status) {
        return StaffMember.reconstitute(
                STAFF_ID,
                FULL_NAME,
                "ada@example.com",
                HIRE_DATE,
                "Engineering",
                "manager-1",
                "Software Engineer",
                ROLE_START_DATE,
                "Level 2",
                "Permanent",
                status
        );
    }

    private StaffMember staffMemberWith(String field, String value) {
        return new StaffMember(
                STAFF_ID,
                FULL_NAME,
                field.equals("email") ? value : "ada@example.com",
                HIRE_DATE,
                field.equals("department") ? value : "Engineering",
                field.equals("managerId") ? value : "manager-1",
                field.equals("jobRole") ? value : "Software Engineer",
                ROLE_START_DATE,
                field.equals("jobLevel") ? value : "Level 2",
                field.equals("employmentType") ? value : "Permanent"
        );
    }

    private static Stream<Arguments> invalidRequiredStrings() {
        return Stream.of(
                invalidStrings("email", StaffMember.EMAIL_NOT_EMPTY),
                invalidStrings("department", StaffMember.DEPARTMENT_NOT_EMPTY),
                invalidStrings("managerId", StaffMember.MANAGER_ID_NOT_EMPTY),
                invalidStrings("jobRole", StaffMember.JOB_ROLE_NOT_EMPTY),
                invalidStrings("jobLevel", StaffMember.JOB_LEVEL_NOT_EMPTY),
                invalidStrings("employmentType", StaffMember.EMPLOYMENT_TYPE_NOT_EMPTY)
        ).flatMap(stream -> stream);
    }

    private static Stream<Arguments> invalidStrings(String field, String message) {
        return Stream.of(
                Arguments.of(field, null, message),
                Arguments.of(field, "", message),
                Arguments.of(field, "   ", message)
        );
    }
}
