package uk.ac.staffs.leavebooking.staff.infrastructure.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import uk.ac.staffs.leavebooking.staff.domain.EmploymentStatus;
import uk.ac.staffs.leavebooking.staff.infrastructure.entities.StaffMemberJpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@DisplayName("Staff Member Repository")
class StaffMemberRepositoryTests {
    private static final LocalDate HIRE_DATE = LocalDate.of(2024, 4, 1);
    private static final LocalDate ROLE_START_DATE = LocalDate.of(2025, 8, 1);

    @Autowired
    private StaffMemberRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("A complete staff member and employment status survive a database round-trip")
    void saveAndReloadPreservesEveryField() {
        repository.save(staffMember("staff-1", "ada@example.com", "manager-1", "Engineering"));
        flushAndClear();

        StaffMemberJpa restored = repository.findById("staff-1").orElseThrow();

        assertEquals("Ada", restored.getFirstName());
        assertEquals("Lovelace", restored.getSurname());
        assertEquals("ada@example.com", restored.getEmail());
        assertEquals(HIRE_DATE, restored.getHireDate());
        assertEquals("Engineering", restored.getDepartment());
        assertEquals("manager-1", restored.getManagerId());
        assertEquals("Software Engineer", restored.getJobRole());
        assertEquals(ROLE_START_DATE, restored.getRoleStartDate());
        assertEquals("Level 2", restored.getJobLevel());
        assertEquals("Permanent", restored.getEmploymentType());
        assertEquals(EmploymentStatus.ON_LEAVE, restored.getEmploymentStatus());
    }

    @Test
    @DisplayName("Email lookup returns the matching staff member")
    void findByEmailReturnsMatchingStaffMember() {
        repository.save(staffMember("staff-1", "ada@example.com", "manager-1", "Engineering"));
        flushAndClear();

        StaffMemberJpa restored = repository.findByEmail("ada@example.com").orElseThrow();

        assertEquals("staff-1", restored.getId());
    }

    @Test
    @DisplayName("Email existence reports present and absent email addresses")
    void existsByEmailReportsPresence() {
        repository.save(staffMember("staff-1", "ada@example.com", "manager-1", "Engineering"));
        flushAndClear();

        assertTrue(repository.existsByEmail("ada@example.com"));
        assertFalse(repository.existsByEmail("grace@example.com"));
    }

    @Test
    @DisplayName("Manager lookup returns only assigned staff members")
    void findByManagerIdReturnsAssignedStaff() {
        repository.save(staffMember("staff-1", "ada@example.com", "manager-1", "Engineering"));
        repository.save(staffMember("staff-2", "grace@example.com", "manager-1", "Finance"));
        repository.save(staffMember("staff-3", "alan@example.com", "manager-2", "Engineering"));
        flushAndClear();

        List<StaffMemberJpa> results = repository.findByManagerId("manager-1");

        assertEquals(
                Set.of("staff-1", "staff-2"),
                results.stream().map(StaffMemberJpa::getId).collect(Collectors.toSet())
        );
    }

    @Test
    @DisplayName("Department lookup returns only staff in that department")
    void findByDepartmentReturnsMatchingStaff() {
        repository.save(staffMember("staff-1", "ada@example.com", "manager-1", "Engineering"));
        repository.save(staffMember("staff-2", "grace@example.com", "manager-1", "Finance"));
        repository.save(staffMember("staff-3", "alan@example.com", "manager-2", "Engineering"));
        flushAndClear();

        List<StaffMemberJpa> results = repository.findByDepartment("Engineering");

        assertEquals(
                Set.of("staff-1", "staff-3"),
                results.stream().map(StaffMemberJpa::getId).collect(Collectors.toSet())
        );
    }

    @Test
    @DisplayName("The database prevents duplicate staff email addresses")
    void duplicateEmailIsRejected() {
        repository.save(staffMember("staff-1", "ada@example.com", "manager-1", "Engineering"));
        entityManager.flush();
        repository.save(staffMember("staff-2", "ada@example.com", "manager-2", "Finance"));

        assertThrows(PersistenceException.class, entityManager::flush);
    }

    @Test
    @DisplayName("The database rejects a role start date before the hire date")
    void schemaRejectsRoleStartBeforeHire() {
        StaffMemberJpa invalid = staffMember(
                "staff-1",
                "ada@example.com",
                "manager-1",
                "Engineering"
        );
        invalid.setRoleStartDate(HIRE_DATE.minusDays(1));
        repository.save(invalid);

        assertThrows(PersistenceException.class, entityManager::flush);
    }

    private StaffMemberJpa staffMember(
            String id,
            String email,
            String managerId,
            String department
    ) {
        return new StaffMemberJpa(
                id,
                "Ada",
                "Lovelace",
                email,
                HIRE_DATE,
                department,
                managerId,
                "Software Engineer",
                ROLE_START_DATE,
                "Level 2",
                "Permanent",
                EmploymentStatus.ON_LEAVE
        );
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
