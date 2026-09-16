package uk.ac.staffs.leavebooking.leave.application.mappers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveRequestDTO;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveRequestJpa;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Leave Request database To responseMapper")
class LeaveRequestJpaToDTOMapperTests {
    @Test
    @DisplayName("Every persisted leave-request field maps to the query DTO")
    void everyFieldIsMapped() {
        LeaveRequestJpa jpa = new LeaveRequestJpa(
                "request-1",
                "staff-1",
                "manager-1",
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 14),
                "Summer holiday",
                LeaveType.ANNUAL,
                LeaveStatus.APPROVED
        );

        LeaveRequestDTO dto = LeaveRequestJpaToDTOMapper.map(jpa);

        assertEquals("request-1", dto.id());
        assertEquals("staff-1", dto.staffMemberId());
        assertEquals("manager-1", dto.managerId());
        assertEquals(LocalDate.of(2026, 8, 10), dto.startDate());
        assertEquals(LocalDate.of(2026, 8, 14), dto.endDate());
        assertEquals("Summer holiday", dto.reason());
        assertEquals(LeaveType.ANNUAL, dto.leaveType());
        assertEquals(LeaveStatus.APPROVED, dto.status());
    }

    @Test
    @DisplayName("A null leave-request JPA entity cannot be mapped to a DTO")
    void nullJpaEntityIsRejected() {
        Throwable exception = assertThrows(NullPointerException.class, () ->
                LeaveRequestJpaToDTOMapper.map(null)
        );

        assertEquals(LeaveRequestJpaToDTOMapper.LEAVE_REQUEST_JPA_NOT_NULL, exception.getMessage());
    }

    @Test
    @DisplayName("A request pending HR approval maps to the query DTO without reconstruction")
    void pendingHrApprovalStatusMapsToDto() {
        LeaveRequestJpa jpa = new LeaveRequestJpa(
                "request-1",
                "staff-1",
                "manager-1",
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 14),
                "Summer holiday",
                LeaveType.ANNUAL,
                LeaveStatus.PENDING_HR_APPROVAL
        );

        LeaveRequestDTO dto = LeaveRequestJpaToDTOMapper.map(jpa);

        assertEquals(LeaveStatus.PENDING_HR_APPROVAL, dto.status());
    }
}
