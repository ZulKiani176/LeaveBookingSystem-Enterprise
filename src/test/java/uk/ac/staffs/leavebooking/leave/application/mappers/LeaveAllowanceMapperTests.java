package uk.ac.staffs.leavebooking.leave.application.mappers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.staffs.leavebooking.common.FullName;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.leave.domain.BusinessYear;
import uk.ac.staffs.leavebooking.leave.domain.LeaveAllowance;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceJpa;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Leave Allowance Mapper")
class LeaveAllowanceMapperTests {
    private static final Identity<LeaveAllowance> ALLOWANCE_ID = Identity.of("leave-allowance-id");
    private static final FullName FULL_NAME = new FullName("Ada", "Lovelace");
    private static final BusinessYear BUSINESS_YEAR = new BusinessYear(
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2027, 3, 31)
    );

    @Test
    @DisplayName("A new full allowance survives a domain-JPA-domain round-trip")
    void newFullAllowanceSurvivesRoundTrip() {
        LeaveAllowance original = newAllowance(25);

        LeaveAllowance restored = roundTrip(original);

        assertAllowanceState(restored, 25, 25, 0);
    }

    @Test
    @DisplayName("A partially used allowance survives a domain-JPA-domain round-trip")
    void partiallyUsedAllowanceSurvivesRoundTrip() {
        LeaveAllowance original = LeaveAllowance.reconstitute(
                ALLOWANCE_ID,
                "staff-member-id",
                FULL_NAME,
                "manager-id",
                BUSINESS_YEAR,
                25,
                17
        );

        LeaveAllowance restored = roundTrip(original);

        assertAllowanceState(restored, 25, 17, 8);
    }

    @Test
    @DisplayName("An amended allowance survives a domain-JPA-domain round-trip")
    void amendedAllowanceSurvivesRoundTrip() {
        LeaveAllowance original = newAllowance(25);
        original.deduct(5);
        original.amendEntitlement(30);

        LeaveAllowance restored = roundTrip(original);

        assertAllowanceState(restored, 30, 25, 5);
    }

    @Test
    @DisplayName("Full name and business year are flattened into persistence fields")
    void valueObjectsAreFlattenedIntoPersistenceFields() {
        LeaveAllowanceJpa jpa = LeaveAllowanceDomainToJpaMapper.map(newAllowance(25));

        assertEquals("Ada", jpa.getFirstName());
        assertEquals("Lovelace", jpa.getSurname());
        assertEquals(BUSINESS_YEAR.startDate(), jpa.getBusinessYearStart());
        assertEquals(BUSINESS_YEAR.endDate(), jpa.getBusinessYearEnd());
    }

    @Test
    @DisplayName("A null domain allowance cannot be mapped to persistence")
    void nullDomainAllowanceIsRejected() {
        Throwable exception = assertThrows(NullPointerException.class, () ->
                LeaveAllowanceDomainToJpaMapper.map(null)
        );

        assertEquals(LeaveAllowanceDomainToJpaMapper.LEAVE_ALLOWANCE_NOT_NULL, exception.getMessage());
    }

    @Test
    @DisplayName("A null persistence allowance cannot be mapped to the domain")
    void nullJpaAllowanceIsRejected() {
        Throwable exception = assertThrows(NullPointerException.class, () ->
                LeaveAllowanceJpaToDomainMapper.map(null)
        );

        assertEquals(LeaveAllowanceJpaToDomainMapper.LEAVE_ALLOWANCE_JPA_NOT_NULL, exception.getMessage());
    }

    private LeaveAllowance roundTrip(LeaveAllowance original) {
        LeaveAllowanceJpa jpa = LeaveAllowanceDomainToJpaMapper.map(original);
        return LeaveAllowanceJpaToDomainMapper.map(jpa);
    }

    private void assertAllowanceState(
            LeaveAllowance allowance,
            int annualEntitlement,
            int remainingDays,
            int usedDays
    ) {
        assertEquals(ALLOWANCE_ID, allowance.id());
        assertEquals("staff-member-id", allowance.staffMemberId());
        assertEquals(FULL_NAME, allowance.fullName());
        assertEquals("manager-id", allowance.managerId());
        assertEquals(BUSINESS_YEAR, allowance.businessYear());
        assertEquals(annualEntitlement, allowance.annualEntitlement());
        assertEquals(remainingDays, allowance.remainingDays());
        assertEquals(usedDays, allowance.usedDays());
    }

    private LeaveAllowance newAllowance(int annualEntitlement) {
        return new LeaveAllowance(
                ALLOWANCE_ID,
                "staff-member-id",
                FULL_NAME,
                "manager-id",
                BUSINESS_YEAR,
                annualEntitlement
        );
    }
}
