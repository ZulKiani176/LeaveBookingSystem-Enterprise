package uk.ac.staffs.leavebooking.leave.application.mappers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveAllowanceDTO;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceJpa;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Leave Allowance database To responseMapper")
class LeaveAllowanceJpaToDTOMapperTests {
    @Test
    @DisplayName("Every persisted allowance field maps to the query DTO and used days are derived")
    void everyFieldIsMappedAndUsedDaysAreDerived() {
        LeaveAllowanceJpa jpa = new LeaveAllowanceJpa(
                "allowance-1",
                "staff-1",
                "Ada",
                "Lovelace",
                "manager-1",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2027, 3, 31),
                30,
                22
        );

        LeaveAllowanceDTO dto = LeaveAllowanceJpaToDTOMapper.map(jpa);

        assertEquals("allowance-1", dto.id());
        assertEquals("staff-1", dto.staffMemberId());
        assertEquals("Ada", dto.firstName());
        assertEquals("Lovelace", dto.surname());
        assertEquals("manager-1", dto.managerId());
        assertEquals(LocalDate.of(2026, 4, 1), dto.businessYearStart());
        assertEquals(LocalDate.of(2027, 3, 31), dto.businessYearEnd());
        assertEquals(30, dto.annualEntitlement());
        assertEquals(java.math.BigDecimal.valueOf(22), dto.remainingDays());
        assertEquals(java.math.BigDecimal.valueOf(8), dto.usedDays());
    }

    @Test
    @DisplayName("A null allowance JPA entity cannot be mapped to a DTO")
    void nullJpaEntityIsRejected() {
        Throwable exception = assertThrows(NullPointerException.class, () ->
                LeaveAllowanceJpaToDTOMapper.map(null)
        );

        assertEquals(LeaveAllowanceJpaToDTOMapper.LEAVE_ALLOWANCE_JPA_NOT_NULL, exception.getMessage());
    }
}
