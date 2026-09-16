package uk.ac.staffs.leavebooking.leave.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.staffs.leavebooking.common.FullName;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveAllowanceException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Annual entitlement and carry-over")
class LeaveAllowanceTask17Tests {
    @Test
    @DisplayName("Half-day deduction and restoration remain exact")
    void halfDayDeductionAndRestorationAreExact() {
        LeaveAllowance allowance = allowance(LeaveDays.of("25.0"));

        allowance.deduct(LeaveDays.of("0.5"));
        assertEquals(LeaveDays.of("24.5"), allowance.remainingLeaveDays());
        assertEquals(LeaveDays.of("0.5"), allowance.usedLeaveDays());

        allowance.restore(LeaveDays.of("0.5"));
        assertEquals(LeaveDays.of("25.0"), allowance.remainingLeaveDays());
    }

    @Test
    @DisplayName("Carry-over is separate from contractual base entitlement")
    void carryOverIsSeparateFromBaseEntitlement() {
        LeaveAllowance allowance = allowance(LeaveDays.of("25.0"));

        allowance.setCarryOver(LeaveDays.of("3.5"));

        assertEquals(LeaveDays.of("25.0"), allowance.baseEntitlement());
        assertEquals(LeaveDays.of("3.5"), allowance.carriedOverDays());
        assertEquals(LeaveDays.of("28.5"), allowance.totalEntitlement());
        assertEquals(LeaveDays.of("28.5"), allowance.remainingLeaveDays());
    }

    @Test
    @DisplayName("Carry-over preserves leave already used")
    void carryOverPreservesUsedLeave() {
        LeaveAllowance allowance = allowance(LeaveDays.of("25.0"));
        allowance.deduct(LeaveDays.of("4.5"));

        allowance.setCarryOver(LeaveDays.of("5.0"));

        assertEquals(LeaveDays.of("4.5"), allowance.usedLeaveDays());
        assertEquals(LeaveDays.of("25.5"), allowance.remainingLeaveDays());
    }

    @Test
    @DisplayName("Setting the same carry-over twice does not double-add it")
    void repeatedCarryOverIsIdempotent() {
        LeaveAllowance allowance = allowance(LeaveDays.of("25.0"));

        allowance.setCarryOver(LeaveDays.of("5.0"));
        allowance.setCarryOver(LeaveDays.of("5.0"));

        assertEquals(LeaveDays.of("5.0"), allowance.carriedOverDays());
        assertEquals(LeaveDays.of("30.0"), allowance.totalEntitlement());
    }

    @Test
    @DisplayName("Carry-over above the prototype cap is rejected without mutation")
    void carryOverAboveMaximumIsRejected() {
        LeaveAllowance allowance = allowance(LeaveDays.of("25.0"));

        assertThrows(
                InvalidLeaveAllowanceException.class,
                () -> allowance.setCarryOver(LeaveDays.of("5.5"))
        );

        assertEquals(LeaveDays.zero(), allowance.carriedOverDays());
        assertEquals(LeaveDays.of("25.0"), allowance.remainingLeaveDays());
    }

    @Test
    @DisplayName("Reconstitution preserves fractional base carry remaining and used values")
    void reconstitutionPreservesExactAllowanceState() {
        LeaveAllowance allowance = LeaveAllowance.reconstitute(
                Identity.of("allowance-1"),
                "staff-1",
                new FullName("Ada", "Lovelace"),
                "manager-1",
                businessYear(),
                LeaveDays.of("24.5"),
                LeaveDays.of("3.5"),
                LeaveDays.of("22.0")
        );

        assertEquals(LeaveDays.of("28.0"), allowance.totalEntitlement());
        assertEquals(LeaveDays.of("6.0"), allowance.usedLeaveDays());
    }

    private LeaveAllowance allowance(LeaveDays baseEntitlement) {
        return new LeaveAllowance(
                Identity.of("allowance-1"),
                "staff-1",
                new FullName("Ada", "Lovelace"),
                "manager-1",
                businessYear(),
                baseEntitlement
        );
    }

    private BusinessYear businessYear() {
        return new BusinessYear(
                LocalDate.parse("2026-04-01"),
                LocalDate.parse("2027-03-31")
        );
    }
}
