package uk.ac.staffs.leavebooking.leave.infrastructure.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import uk.ac.staffs.leavebooking.common.FullName;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.leave.application.mappers.LeaveAllowanceDomainToJpaMapper;
import uk.ac.staffs.leavebooking.leave.application.mappers.LeaveAllowanceJpaToDomainMapper;
import uk.ac.staffs.leavebooking.leave.domain.BusinessYear;
import uk.ac.staffs.leavebooking.leave.domain.LeaveAllowance;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceJpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@DisplayName("Leave Allowance Repository")
class LeaveAllowanceRepositoryTests {
    private static final FullName FULL_NAME = new FullName("Ada", "Lovelace");
    private static final LocalDate FIRST_YEAR_START = LocalDate.of(2026, 4, 1);
    private static final LocalDate FIRST_YEAR_END = LocalDate.of(2027, 3, 31);
    private static final LocalDate SECOND_YEAR_START = LocalDate.of(2027, 4, 1);
    private static final LocalDate SECOND_YEAR_END = LocalDate.of(2028, 3, 31);
    private static final BusinessYear FIRST_BUSINESS_YEAR =
            new BusinessYear(FIRST_YEAR_START, FIRST_YEAR_END);

    @Autowired
    private LeaveAllowanceRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("A leave allowance can be saved and reloaded by identity")
    void saveAndFindById() {
        repository.save(allowance("allowance-1", "staff-1", FIRST_YEAR_START, FIRST_YEAR_END, 25, 25));
        flushAndClear();

        LeaveAllowanceJpa restored = repository.findById("allowance-1").orElseThrow();

        assertEquals("allowance-1", restored.getId());
        assertEquals("staff-1", restored.getStaffMemberId());
        assertEquals("Ada", restored.getFirstName());
        assertEquals("Lovelace", restored.getSurname());
        assertEquals("manager-1", restored.getManagerId());
        assertEquals(FIRST_YEAR_START, restored.getBusinessYearStart());
        assertEquals(FIRST_YEAR_END, restored.getBusinessYearEnd());
        assertEquals(25, restored.getAnnualEntitlement());
        assertEquals(25, restored.getRemainingDays());
    }

    @Test
    @DisplayName("The same staff member can have allowances for different business years")
    void sameStaffMemberCanHaveAllowancesForDifferentBusinessYears() {
        repository.save(allowance("allowance-1", "staff-1", FIRST_YEAR_START, FIRST_YEAR_END, 25, 25));
        repository.save(allowance("allowance-2", "staff-1", SECOND_YEAR_START, SECOND_YEAR_END, 30, 30));
        flushAndClear();

        assertEquals(2, repository.count());
    }

    @Test
    @DisplayName("Staff identity lookup returns all yearly allowance records")
    void findByStaffMemberIdReturnsMultipleYearlyRecords() {
        repository.save(allowance("allowance-1", "staff-1", FIRST_YEAR_START, FIRST_YEAR_END, 25, 25));
        repository.save(allowance("allowance-2", "staff-1", SECOND_YEAR_START, SECOND_YEAR_END, 30, 30));
        repository.save(allowance("allowance-3", "staff-2", FIRST_YEAR_START, FIRST_YEAR_END, 20, 20));
        flushAndClear();

        List<LeaveAllowanceJpa> results = repository.findByStaffMemberId("staff-1");

        assertEquals(2, results.size());
        assertEquals(
                Set.of("allowance-1", "allowance-2"),
                results.stream().map(LeaveAllowanceJpa::getId).collect(Collectors.toSet())
        );
    }

    @Test
    @DisplayName("A specific staff business-year allowance can be found")
    void findByStaffMemberIdAndBusinessYear() {
        repository.save(allowance("allowance-1", "staff-1", FIRST_YEAR_START, FIRST_YEAR_END, 25, 25));
        repository.save(allowance("allowance-2", "staff-1", SECOND_YEAR_START, SECOND_YEAR_END, 30, 30));
        flushAndClear();

        LeaveAllowanceJpa restored = repository
                .findByStaffMemberIdAndBusinessYearStartAndBusinessYearEnd(
                        "staff-1",
                        SECOND_YEAR_START,
                        SECOND_YEAR_END
                )
                .orElseThrow();

        assertEquals("allowance-2", restored.getId());
    }

    @Test
    @DisplayName("System usage lookup returns allowances from only the exact business year")
    void findByExactBusinessYearExcludesOtherYears() {
        repository.save(allowance("allowance-1", "staff-1", FIRST_YEAR_START, FIRST_YEAR_END, 25, 20));
        repository.save(allowance("allowance-2", "staff-2", FIRST_YEAR_START, FIRST_YEAR_END, 30, 18));
        repository.save(allowance("allowance-3", "staff-3", SECOND_YEAR_START, SECOND_YEAR_END, 28, 28));
        flushAndClear();

        List<LeaveAllowanceJpa> results =
                repository.findByBusinessYearStartAndBusinessYearEnd(
                        FIRST_YEAR_START,
                        FIRST_YEAR_END
                );

        assertEquals(
                Set.of("allowance-1", "allowance-2"),
                results.stream().map(LeaveAllowanceJpa::getId).collect(Collectors.toSet())
        );
    }

    @Test
    @DisplayName("A partially used allowance survives a database round-trip")
    void partiallyUsedAllowanceSurvivesDatabaseRoundTrip() {
        LeaveAllowance original = LeaveAllowance.reconstitute(
                Identity.of("allowance-1"),
                "staff-1",
                FULL_NAME,
                "manager-1",
                FIRST_BUSINESS_YEAR,
                30,
                22
        );
        repository.save(LeaveAllowanceDomainToJpaMapper.map(original));
        flushAndClear();

        LeaveAllowance restored = repository.findById("allowance-1")
                .map(LeaveAllowanceJpaToDomainMapper::map)
                .orElseThrow();

        assertEquals(FULL_NAME, restored.fullName());
        assertEquals(FIRST_BUSINESS_YEAR, restored.businessYear());
        assertEquals(30, restored.annualEntitlement());
        assertEquals(22, restored.remainingDays());
        assertEquals(8, restored.usedDays());
    }

    @Test
    @DisplayName("Deduction, restoration and amended entitlement survive database reload")
    void changedAllowanceStateSurvivesDatabaseReload() {
        LeaveAllowance original = new LeaveAllowance(
                Identity.of("allowance-1"),
                "staff-1",
                FULL_NAME,
                "manager-1",
                FIRST_BUSINESS_YEAR,
                25
        );
        original.deduct(10);
        original.restore(2);
        original.amendEntitlement(30);
        repository.save(LeaveAllowanceDomainToJpaMapper.map(original));
        flushAndClear();

        LeaveAllowance restored = repository.findById("allowance-1")
                .map(LeaveAllowanceJpaToDomainMapper::map)
                .orElseThrow();

        assertEquals(30, restored.annualEntitlement());
        assertEquals(22, restored.remainingDays());
        assertEquals(8, restored.usedDays());
    }

    @Test
    @DisplayName("The database prevents duplicate allowances for the same staff business year")
    void duplicateStaffBusinessYearIsRejected() {
        repository.save(allowance("allowance-1", "staff-1", FIRST_YEAR_START, FIRST_YEAR_END, 25, 25));
        entityManager.flush();
        repository.save(allowance("allowance-2", "staff-1", FIRST_YEAR_START, FIRST_YEAR_END, 30, 30));

        assertThrows(PersistenceException.class, entityManager::flush);
    }

    @Test
    @DisplayName("The database rejects a remaining balance above entitlement")
    void remainingDaysAboveEntitlementIsRejectedBySchema() {
        repository.save(allowance("allowance-1", "staff-1", FIRST_YEAR_START, FIRST_YEAR_END, 25, 26));

        assertThrows(PersistenceException.class, entityManager::flush);
    }

    @Test
    @DisplayName("The database rejects a business-year end before its start")
    void reversedBusinessYearIsRejectedBySchema() {
        repository.save(allowance("allowance-1", "staff-1", FIRST_YEAR_END, FIRST_YEAR_START, 25, 25));

        assertThrows(PersistenceException.class, entityManager::flush);
    }

    private LeaveAllowanceJpa allowance(
            String id,
            String staffMemberId,
            LocalDate businessYearStart,
            LocalDate businessYearEnd,
            int annualEntitlement,
            int remainingDays
    ) {
        return new LeaveAllowanceJpa(
                id,
                staffMemberId,
                "Ada",
                "Lovelace",
                "manager-1",
                businessYearStart,
                businessYearEnd,
                annualEntitlement,
                remainingDays
        );
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
