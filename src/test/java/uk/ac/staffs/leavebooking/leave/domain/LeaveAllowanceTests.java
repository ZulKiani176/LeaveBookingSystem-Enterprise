package uk.ac.staffs.leavebooking.leave.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.ac.staffs.leavebooking.common.AggregateRoot;
import uk.ac.staffs.leavebooking.common.FullName;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveAllowanceException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Leave allowance rules")
class LeaveAllowanceTests {
    private static final Identity<LeaveAllowance> ALLOWANCE_ID = Identity.of("leave-allowance-id");
    private static final String STAFF_MEMBER_ID = "staff-member-id";
    private static final FullName FULL_NAME = new FullName("Ada", "Lovelace");
    private static final String MANAGER_ID = "manager-id";
    private static final BusinessYear BUSINESS_YEAR = new BusinessYear(
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2027, 3, 31)
    );
    private static final int ANNUAL_ENTITLEMENT = 25;

    @Test
    @DisplayName("A new allowance retains its staff details and starts with its full entitlement")
    void newAllowanceRetainsDetailsAndFullBalance() {
        LeaveAllowance allowance = validAllowance();

        assertAll(
                () -> assertTrue(allowance instanceof AggregateRoot),
                () -> assertEquals(ALLOWANCE_ID, allowance.id()),
                () -> assertEquals(STAFF_MEMBER_ID, allowance.staffMemberId()),
                () -> assertEquals(FULL_NAME, allowance.fullName()),
                () -> assertEquals(MANAGER_ID, allowance.managerId()),
                () -> assertEquals(BUSINESS_YEAR, allowance.businessYear()),
                () -> assertEquals(ANNUAL_ENTITLEMENT, allowance.annualEntitlement()),
                () -> assertEquals(ANNUAL_ENTITLEMENT, allowance.remainingDays()),
                () -> assertEquals(0, allowance.usedDays())
        );
    }

    @Test
    @DisplayName("An allowance trims its staff and manager identities")
    void surroundingWhitespaceInIdentitiesIsTrimmed() {
        LeaveAllowance allowance = new LeaveAllowance(
                ALLOWANCE_ID,
                "  staff-member-id  ",
                FULL_NAME,
                "  manager-id  ",
                BUSINESS_YEAR,
                ANNUAL_ENTITLEMENT
        );

        assertAll(
                () -> assertEquals(STAFF_MEMBER_ID, allowance.staffMemberId()),
                () -> assertEquals(MANAGER_ID, allowance.managerId())
        );
    }

    @Test
    @DisplayName("Zero annual entitlement is valid")
    void zeroAnnualEntitlementIsValid() {
        LeaveAllowance allowance = new LeaveAllowance(
                ALLOWANCE_ID,
                STAFF_MEMBER_ID,
                FULL_NAME,
                MANAGER_ID,
                BUSINESS_YEAR,
                0
        );

        assertEquals(0, allowance.annualEntitlement());
        assertEquals(0, allowance.remainingDays());
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("A null, empty or blank staff member identity is rejected")
    void invalidStaffMemberIdIsRejected(String invalidStaffMemberId) {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                new LeaveAllowance(
                        ALLOWANCE_ID,
                        invalidStaffMemberId,
                        FULL_NAME,
                        MANAGER_ID,
                        BUSINESS_YEAR,
                        ANNUAL_ENTITLEMENT
                )
        );

        assertEquals(LeaveAllowance.STAFF_MEMBER_ID_NOT_EMPTY, exception.getMessage());
    }

    @Test
    @DisplayName("A null staff member full name is rejected")
    void nullFullNameIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                new LeaveAllowance(
                        ALLOWANCE_ID,
                        STAFF_MEMBER_ID,
                        null,
                        MANAGER_ID,
                        BUSINESS_YEAR,
                        ANNUAL_ENTITLEMENT
                )
        );

        assertEquals(LeaveAllowance.FULL_NAME_NOT_NULL, exception.getMessage());
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("A null, empty or blank manager identity is rejected")
    void invalidManagerIdIsRejected(String invalidManagerId) {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                new LeaveAllowance(
                        ALLOWANCE_ID,
                        STAFF_MEMBER_ID,
                        FULL_NAME,
                        invalidManagerId,
                        BUSINESS_YEAR,
                        ANNUAL_ENTITLEMENT
                )
        );

        assertEquals(LeaveAllowance.MANAGER_ID_NOT_EMPTY, exception.getMessage());
    }

    @Test
    @DisplayName("A null business year is rejected")
    void nullBusinessYearIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                new LeaveAllowance(
                        ALLOWANCE_ID,
                        STAFF_MEMBER_ID,
                        FULL_NAME,
                        MANAGER_ID,
                        null,
                        ANNUAL_ENTITLEMENT
                )
        );

        assertEquals(LeaveAllowance.BUSINESS_YEAR_NOT_NULL, exception.getMessage());
    }

    @Test
    @DisplayName("A negative annual entitlement is rejected")
    void negativeAnnualEntitlementIsRejected() {
        Throwable exception = assertThrows(InvalidLeaveAllowanceException.class, () ->
                new LeaveAllowance(
                        ALLOWANCE_ID,
                        STAFF_MEMBER_ID,
                        FULL_NAME,
                        MANAGER_ID,
                        BUSINESS_YEAR,
                        -1
                )
        );

        assertEquals(LeaveAllowance.ANNUAL_ENTITLEMENT_NOT_NEGATIVE, exception.getMessage());
    }

    @Test
    @DisplayName("Deducting leave reduces the balance and increases used days")
    void deductionUpdatesRemainingAndUsedDays() {
        LeaveAllowance allowance = validAllowance();

        allowance.deduct(5);

        assertAll(
                () -> assertEquals(20, allowance.remainingDays()),
                () -> assertEquals(5, allowance.usedDays())
        );
    }

    @Test
    @DisplayName("The entire remaining allowance can be deducted")
    void entireRemainingAllowanceCanBeDeducted() {
        LeaveAllowance allowance = validAllowance();

        allowance.deduct(ANNUAL_ENTITLEMENT);

        assertEquals(0, allowance.remainingDays());
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @ValueSource(ints = {0, -1})
    @DisplayName("A non-positive deduction is rejected without changing the balance")
    void nonPositiveDeductionIsRejected(int invalidDays) {
        LeaveAllowance allowance = validAllowance();

        Throwable exception = assertThrows(InvalidLeaveAllowanceException.class, () ->
                allowance.deduct(invalidDays)
        );

        assertEquals(LeaveAllowance.DEDUCTION_NOT_POSITIVE, exception.getMessage());
        assertEquals(ANNUAL_ENTITLEMENT, allowance.remainingDays());
    }

    @Test
    @DisplayName("A deduction greater than the remaining allowance is rejected without changing the balance")
    void deductionGreaterThanRemainingAllowanceIsRejected() {
        LeaveAllowance allowance = validAllowance();

        Throwable exception = assertThrows(InvalidLeaveAllowanceException.class, () ->
                allowance.deduct(ANNUAL_ENTITLEMENT + 1)
        );

        assertEquals(LeaveAllowance.INSUFFICIENT_REMAINING_ALLOWANCE, exception.getMessage());
        assertEquals(ANNUAL_ENTITLEMENT, allowance.remainingDays());
    }

    @Test
    @DisplayName("Restoring leave increases the balance and reduces used days")
    void restorationUpdatesRemainingAndUsedDays() {
        LeaveAllowance allowance = validAllowance();
        allowance.deduct(10);

        allowance.restore(4);

        assertAll(
                () -> assertEquals(19, allowance.remainingDays()),
                () -> assertEquals(6, allowance.usedDays())
        );
    }

    @Test
    @DisplayName("All used days can be restored")
    void allUsedDaysCanBeRestored() {
        LeaveAllowance allowance = validAllowance();
        allowance.deduct(10);

        allowance.restore(10);

        assertEquals(ANNUAL_ENTITLEMENT, allowance.remainingDays());
        assertEquals(0, allowance.usedDays());
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @ValueSource(ints = {0, -1})
    @DisplayName("A non-positive restoration is rejected without changing the balance")
    void nonPositiveRestorationIsRejected(int invalidDays) {
        LeaveAllowance allowance = validAllowance();
        allowance.deduct(5);

        Throwable exception = assertThrows(InvalidLeaveAllowanceException.class, () ->
                allowance.restore(invalidDays)
        );

        assertEquals(LeaveAllowance.RESTORE_NOT_POSITIVE, exception.getMessage());
        assertEquals(20, allowance.remainingDays());
    }

    @Test
    @DisplayName("Restoration beyond the annual entitlement is rejected without changing the balance")
    void restorationBeyondEntitlementIsRejected() {
        LeaveAllowance allowance = validAllowance();
        allowance.deduct(5);

        Throwable exception = assertThrows(InvalidLeaveAllowanceException.class, () ->
                allowance.restore(6)
        );

        assertEquals(LeaveAllowance.RESTORE_EXCEEDS_ENTITLEMENT, exception.getMessage());
        assertEquals(20, allowance.remainingDays());
    }

    @Test
    @DisplayName("Increasing entitlement preserves used days")
    void increasingEntitlementPreservesUsedDays() {
        LeaveAllowance allowance = validAllowance();
        allowance.deduct(5);

        allowance.amendEntitlement(30);

        assertAll(
                () -> assertEquals(5, allowance.usedDays()),
                () -> assertEquals(30, allowance.annualEntitlement()),
                () -> assertEquals(25, allowance.remainingDays())
        );
    }

    @Test
    @DisplayName("Decreasing entitlement above used leave preserves used days")
    void decreasingEntitlementAboveUsedLeavePreservesUsedDays() {
        LeaveAllowance allowance = validAllowance();
        allowance.deduct(5);

        allowance.amendEntitlement(20);

        assertEquals(20, allowance.annualEntitlement());
        assertEquals(15, allowance.remainingDays());
        assertEquals(5, allowance.usedDays());
    }

    @Test
    @DisplayName("Entitlement can be amended to exactly the used leave")
    void entitlementCanBeAmendedToExactlyUsedLeave() {
        LeaveAllowance allowance = validAllowance();
        allowance.deduct(10);

        allowance.amendEntitlement(10);

        assertEquals(10, allowance.annualEntitlement());
        assertEquals(0, allowance.remainingDays());
        assertEquals(10, allowance.usedDays());
    }

    @Test
    @DisplayName("Entitlement below used leave is rejected without changing state")
    void entitlementBelowUsedLeaveIsRejected() {
        LeaveAllowance allowance = validAllowance();
        allowance.deduct(10);

        Throwable exception = assertThrows(InvalidLeaveAllowanceException.class, () ->
                allowance.amendEntitlement(9)
        );

        assertEquals(LeaveAllowance.ENTITLEMENT_BELOW_USED_DAYS, exception.getMessage());
        assertEquals(ANNUAL_ENTITLEMENT, allowance.annualEntitlement());
        assertEquals(15, allowance.remainingDays());
        assertEquals(10, allowance.usedDays());
    }

    @Test
    @DisplayName("A negative amended entitlement is rejected without changing state")
    void negativeAmendedEntitlementIsRejected() {
        LeaveAllowance allowance = validAllowance();

        Throwable exception = assertThrows(InvalidLeaveAllowanceException.class, () ->
                allowance.amendEntitlement(-1)
        );

        assertEquals(LeaveAllowance.ANNUAL_ENTITLEMENT_NOT_NEGATIVE, exception.getMessage());
        assertEquals(ANNUAL_ENTITLEMENT, allowance.annualEntitlement());
        assertEquals(ANNUAL_ENTITLEMENT, allowance.remainingDays());
    }

    @Test
    @DisplayName("Entitlement can be amended to zero when no leave has been used")
    void zeroAmendedEntitlementIsAllowedWhenNoLeaveHasBeenUsed() {
        LeaveAllowance allowance = validAllowance();

        allowance.amendEntitlement(0);

        assertEquals(0, allowance.annualEntitlement());
        assertEquals(0, allowance.remainingDays());
        assertEquals(0, allowance.usedDays());
    }

    @Test
    @DisplayName("A partially used allowance is reconstituted without changing its balance")
    void partiallyUsedAllowanceIsReconstituted() {
        LeaveAllowance allowance = LeaveAllowance.reconstitute(
                ALLOWANCE_ID,
                STAFF_MEMBER_ID,
                FULL_NAME,
                MANAGER_ID,
                BUSINESS_YEAR,
                ANNUAL_ENTITLEMENT,
                17
        );

        assertEquals(ANNUAL_ENTITLEMENT, allowance.annualEntitlement());
        assertEquals(17, allowance.remainingDays());
        assertEquals(8, allowance.usedDays());
        assertEquals(FULL_NAME, allowance.fullName());
        assertEquals(BUSINESS_YEAR, allowance.businessYear());
    }

    @Test
    @DisplayName("A negative remaining balance cannot be reconstituted")
    void negativeReconstitutedRemainingDaysIsRejected() {
        Throwable exception = assertThrows(InvalidLeaveAllowanceException.class, () ->
                LeaveAllowance.reconstitute(
                        ALLOWANCE_ID,
                        STAFF_MEMBER_ID,
                        FULL_NAME,
                        MANAGER_ID,
                        BUSINESS_YEAR,
                        ANNUAL_ENTITLEMENT,
                        -1
                )
        );

        assertEquals(LeaveAllowance.REMAINING_DAYS_NOT_NEGATIVE, exception.getMessage());
    }

    @Test
    @DisplayName("A remaining balance above entitlement cannot be reconstituted")
    void reconstitutedRemainingDaysAboveEntitlementIsRejected() {
        Throwable exception = assertThrows(InvalidLeaveAllowanceException.class, () ->
                LeaveAllowance.reconstitute(
                        ALLOWANCE_ID,
                        STAFF_MEMBER_ID,
                        FULL_NAME,
                        MANAGER_ID,
                        BUSINESS_YEAR,
                        ANNUAL_ENTITLEMENT,
                        ANNUAL_ENTITLEMENT + 1
                )
        );

        assertEquals(LeaveAllowance.REMAINING_DAYS_EXCEED_ENTITLEMENT, exception.getMessage());
    }

    @Test
    @DisplayName("A negative entitlement cannot be reconstituted")
    void negativeReconstitutedEntitlementIsRejected() {
        Throwable exception = assertThrows(InvalidLeaveAllowanceException.class, () ->
                LeaveAllowance.reconstitute(
                        ALLOWANCE_ID,
                        STAFF_MEMBER_ID,
                        FULL_NAME,
                        MANAGER_ID,
                        BUSINESS_YEAR,
                        -1,
                        0
                )
        );

        assertEquals(LeaveAllowance.ANNUAL_ENTITLEMENT_NOT_NEGATIVE, exception.getMessage());
    }

    @Test
    @DisplayName("Allowance reconstitution enforces existing identifier invariants")
    void allowanceReconstitutionEnforcesIdentifierInvariants() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                LeaveAllowance.reconstitute(
                        ALLOWANCE_ID,
                        "   ",
                        FULL_NAME,
                        MANAGER_ID,
                        BUSINESS_YEAR,
                        ANNUAL_ENTITLEMENT,
                        ANNUAL_ENTITLEMENT
                )
        );

        assertEquals(LeaveAllowance.STAFF_MEMBER_ID_NOT_EMPTY, exception.getMessage());
    }

    private LeaveAllowance validAllowance() {
        return new LeaveAllowance(
                ALLOWANCE_ID,
                STAFF_MEMBER_ID,
                FULL_NAME,
                MANAGER_ID,
                BUSINESS_YEAR,
                ANNUAL_ENTITLEMENT
        );
    }
}
