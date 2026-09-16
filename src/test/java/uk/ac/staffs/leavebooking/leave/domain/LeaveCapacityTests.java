package uk.ac.staffs.leavebooking.leave.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.staffs.leavebooking.common.FullName;
import uk.ac.staffs.leavebooking.common.Identity;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Leave amounts fit their database columns")
class LeaveCapacityTests {
    @Test
    @DisplayName("The largest supported half-day amount is accepted")
    void capacityBoundaryIsAccepted() {
        assertEquals("9999.5", LeaveDays.of("9999.5").value().toPlainString());
    }

    @Test
    @DisplayName("Amounts larger than the storage capacity are rejected clearly")
    void capacityOverflowIsRejected() {
        var exception = assertThrows(IllegalArgumentException.class, () -> LeaveDays.of("10000.0"));
        assertEquals(LeaveDays.VALUE_EXCEEDS_CAPACITY, exception.getMessage());
    }

    @Test
    @DisplayName("Carry-over cannot overflow the balance or partially change it")
    void carryOverOverflowPreservesAllowance() {
        LeaveAllowance allowance = allowance();

        assertThrows(IllegalArgumentException.class, () -> allowance.setCarryOver(LeaveDays.of("0.5")));

        assertEquals(LeaveDays.zero(), allowance.carriedOverDays());
        assertEquals(LeaveDays.of("9999.5"), allowance.remainingLeaveDays());
    }

    @Test
    @DisplayName("An overflowing entitlement amendment preserves the previous balance")
    void amendmentOverflowPreservesAllowance() {
        LeaveAllowance allowance = allowance();
        allowance.amendBaseEntitlement(LeaveDays.of("9999.0"));
        allowance.setCarryOver(LeaveDays.of("0.5"));

        assertThrows(IllegalArgumentException.class,
                () -> allowance.amendBaseEntitlement(LeaveDays.of("9999.5")));

        assertEquals(LeaveDays.of("9999.0"), allowance.baseEntitlement());
        assertEquals(LeaveDays.of("9999.5"), allowance.remainingLeaveDays());
    }

    private LeaveAllowance allowance() {
        return new LeaveAllowance(Identity.of("allowance"), "staff", new FullName("Ada", "Smith"),
                "manager", new BusinessYear(LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31)),
                LeaveDays.of("9999.5"));
    }
}
