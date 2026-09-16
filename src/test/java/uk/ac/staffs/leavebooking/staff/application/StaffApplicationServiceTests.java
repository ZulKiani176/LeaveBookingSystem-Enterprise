package uk.ac.staffs.leavebooking.staff.application;

import uk.ac.staffs.leavebooking.staff.application.dto.CreateStaffMemberDetails;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.staff.application.dto.StaffMemberDTO;
import uk.ac.staffs.leavebooking.staff.application.exceptions.StaffEmailAlreadyExistsException;
import uk.ac.staffs.leavebooking.staff.application.exceptions.StaffMemberNotFoundException;
import uk.ac.staffs.leavebooking.staff.domain.EmploymentStatus;
import uk.ac.staffs.leavebooking.staff.domain.StaffMember;
import uk.ac.staffs.leavebooking.staff.domain.exceptions.InvalidStaffMemberException;
import uk.ac.staffs.leavebooking.staff.infrastructure.entities.StaffMemberJpa;
import uk.ac.staffs.leavebooking.staff.infrastructure.repositories.StaffMemberRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Staff Application Service")
class StaffApplicationServiceTests {
    private static final LocalDate HIRE_DATE = LocalDate.of(2024, 4, 1);
    private static final LocalDate ROLE_START_DATE = LocalDate.of(2024, 4, 1);

    @Mock
    private StaffMemberRepository repository;

    @InjectMocks
    private StaffApplicationService service;

    @Test
    @DisplayName("Creating staff generates a UUIDv7 identity and saves complete active state")
    void createStaffMemberSavesActiveStaffWithGeneratedIdentity() {
        when(repository.existsByEmail("ada@example.com")).thenReturn(false);

        String result = service.createStaffMember(validDetails());

        ArgumentCaptor<StaffMemberJpa> captor = ArgumentCaptor.forClass(StaffMemberJpa.class);
        verify(repository).save(captor.capture());
        StaffMemberJpa saved = captor.getValue();
        assertEquals(result, saved.getId());
        assertEquals(7, UUID.fromString(result).version());
        assertEquals("Ada", saved.getFirstName());
        assertEquals("Lovelace", saved.getSurname());
        assertEquals("ada@example.com", saved.getEmail());
        assertEquals(HIRE_DATE, saved.getHireDate());
        assertEquals("Engineering", saved.getDepartment());
        assertEquals("manager-1", saved.getManagerId());
        assertEquals("Software Engineer", saved.getJobRole());
        assertEquals(ROLE_START_DATE, saved.getRoleStartDate());
        assertEquals("Level 2", saved.getJobLevel());
        assertEquals("Permanent", saved.getEmploymentType());
        assertEquals(EmploymentStatus.ACTIVE, saved.getEmploymentStatus());
    }

    @Test
    @DisplayName("Duplicate email checking uses the trimmed domain email")
    void duplicateEmailIsRejectedWithoutSave() {
        CreateStaffMemberDetails details = new CreateStaffMemberDetails(
                "Ada",
                "Lovelace",
                "  ada@example.com  ",
                HIRE_DATE,
                "Engineering",
                "manager-1",
                "Software Engineer",
                ROLE_START_DATE,
                "Level 2",
                "Permanent"
        );
        when(repository.existsByEmail("ada@example.com")).thenReturn(true);

        StaffEmailAlreadyExistsException exception = assertThrows(
                StaffEmailAlreadyExistsException.class,
                () -> service.createStaffMember(details)
        );

        assertEquals(
                "A staff member with email ada@example.com already exists",
                exception.getMessage()
        );
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Null creation details are rejected without repository access")
    void nullCreationDetailsAreRejected() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.createStaffMember(null)
        );

        assertEquals(StaffApplicationService.CREATE_STAFF_DETAILS_NOT_NULL, exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Department updates pass through the aggregate and persist trimmed state")
    void changeDepartmentUsesAggregateAndSaves() {
        when(repository.findById("staff-1")).thenReturn(Optional.of(staffJpa()));

        service.changeDepartment("staff-1", "  Finance  ");

        ArgumentCaptor<StaffMemberJpa> captor = ArgumentCaptor.forClass(StaffMemberJpa.class);
        verify(repository).save(captor.capture());
        assertEquals("Finance", captor.getValue().getDepartment());
    }

    @Test
    @DisplayName("An invalid department update propagates and does not save")
    void invalidDepartmentChangeDoesNotSave() {
        when(repository.findById("staff-1")).thenReturn(Optional.of(staffJpa()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.changeDepartment("staff-1", "   ")
        );

        assertEquals(StaffMember.DEPARTMENT_NOT_EMPTY, exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Job-role updates pass through the aggregate and persist both placement values")
    void changeJobRoleUsesAggregateAndSaves() {
        when(repository.findById("staff-1")).thenReturn(Optional.of(staffJpa()));
        LocalDate newStartDate = LocalDate.of(2026, 8, 1);

        service.changeJobRole("staff-1", "  Team Leader  ", newStartDate);

        ArgumentCaptor<StaffMemberJpa> captor = ArgumentCaptor.forClass(StaffMemberJpa.class);
        verify(repository).save(captor.capture());
        assertEquals("Team Leader", captor.getValue().getJobRole());
        assertEquals(newStartDate, captor.getValue().getRoleStartDate());
    }

    @Test
    @DisplayName("An invalid job-role date propagates and does not save")
    void invalidJobRoleChangeDoesNotSave() {
        when(repository.findById("staff-1")).thenReturn(Optional.of(staffJpa()));

        InvalidStaffMemberException exception = assertThrows(
                InvalidStaffMemberException.class,
                () -> service.changeJobRole(
                        "staff-1",
                        "Team Leader",
                        HIRE_DATE.minusDays(1)
                )
        );

        assertEquals(StaffMember.ROLE_START_BEFORE_HIRE, exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("A missing department update target throws and does not save")
    void missingDepartmentUpdateTargetThrows() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        StaffMemberNotFoundException exception = assertThrows(
                StaffMemberNotFoundException.class,
                () -> service.changeDepartment("missing", "Finance")
        );

        assertEquals("Staff member not found: missing", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("A missing job-role update target throws and does not save")
    void missingJobRoleUpdateTargetThrows() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(
                StaffMemberNotFoundException.class,
                () -> service.changeJobRole("missing", "Team Leader", ROLE_START_DATE)
        );

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Finding staff by identity maps persistence directly to a DTO")
    void findStaffMemberByIdReturnsDto() {
        when(repository.findById("staff-1")).thenReturn(Optional.of(staffJpa()));

        StaffMemberDTO result = service.findStaffMemberById("staff-1");

        assertEquals("staff-1", result.id());
        assertEquals("ada@example.com", result.email());
        assertEquals(EmploymentStatus.ACTIVE, result.employmentStatus());
    }

    @Test
    @DisplayName("Finding a missing staff identity throws the application exception")
    void findMissingStaffMemberThrows() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(
                StaffMemberNotFoundException.class,
                () -> service.findStaffMemberById("missing")
        );
    }

    @Test
    @DisplayName("Manager lookup maps every assigned staff record")
    void findStaffByManagerIdReturnsDtos() {
        StaffMemberJpa first = staffJpa();
        StaffMemberJpa second = staffJpa();
        second.setId("staff-2");
        second.setEmail("grace@example.com");
        when(repository.findByManagerId("manager-1")).thenReturn(List.of(first, second));

        List<StaffMemberDTO> result = service.findStaffByManagerId("manager-1");

        assertEquals(2, result.size());
        assertEquals(List.of("staff-1", "staff-2"), result.stream().map(StaffMemberDTO::id).toList());
    }

    @Test
    @DisplayName("Department lookup maps every matching staff record")
    void findStaffByDepartmentReturnsDtos() {
        when(repository.findByDepartment("Engineering")).thenReturn(List.of(staffJpa()));

        List<StaffMemberDTO> result = service.findStaffByDepartment("Engineering");

        assertEquals(1, result.size());
        assertEquals("Engineering", result.getFirst().department());
    }

    @Test
    @DisplayName("Manager lookup returns an empty list rather than null")
    void emptyManagerLookupReturnsEmptyList() {
        when(repository.findByManagerId("manager-1")).thenReturn(List.of());

        List<StaffMemberDTO> result = service.findStaffByManagerId("manager-1");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Department lookup returns an empty list rather than null")
    void emptyDepartmentLookupReturnsEmptyList() {
        when(repository.findByDepartment("Finance")).thenReturn(List.of());

        List<StaffMemberDTO> result = service.findStaffByDepartment("Finance");

        assertTrue(result.isEmpty());
    }

    private CreateStaffMemberDetails validDetails() {
        return new CreateStaffMemberDetails(
                "Ada",
                "Lovelace",
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

    private StaffMemberJpa staffJpa() {
        return new StaffMemberJpa(
                "staff-1",
                "Ada",
                "Lovelace",
                "ada@example.com",
                HIRE_DATE,
                "Engineering",
                "manager-1",
                "Software Engineer",
                ROLE_START_DATE,
                "Level 2",
                "Permanent",
                EmploymentStatus.ACTIVE
        );
    }
}
