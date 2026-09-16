package uk.ac.staffs.leavebooking.leave.application.mappers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.leave.domain.LeavePeriod;
import uk.ac.staffs.leavebooking.leave.domain.LeaveRequest;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveRequestJpa;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Leave Request Mapper")
class LeaveRequestMapperTests {
    private static final Identity<LeaveRequest> REQUEST_ID = Identity.of("leave-request-id");
    private static final LeavePeriod LEAVE_PERIOD = new LeavePeriod(
            LocalDate.of(2026, 10, 12),
            LocalDate.of(2026, 10, 16)
    );

    @ParameterizedTest(name = "{displayName} (case {index})")
    @EnumSource(LeaveStatus.class)
    @DisplayName("All request details survive conversion to the database format and back")
    void requestRoundTripPreservesAllPersistedState(LeaveStatus status) {
        LeaveRequest original = LeaveRequest.reconstitute(
                REQUEST_ID,
                "staff-member-id",
                "manager-id",
                LEAVE_PERIOD,
                "Family holiday",
                LeaveType.ANNUAL,
                status
        );

        LeaveRequestJpa jpa = LeaveRequestDomainToJpaMapper.map(original);
        LeaveRequest restored = LeaveRequestJpaToDomainMapper.map(jpa);

        assertEquals(original.id(), restored.id());
        assertEquals(original.staffMemberId(), restored.staffMemberId());
        assertEquals(original.managerId(), restored.managerId());
        assertEquals(original.leavePeriod(), restored.leavePeriod());
        assertEquals(LEAVE_PERIOD.startDate(), restored.leavePeriod().startDate());
        assertEquals(LEAVE_PERIOD.endDate(), restored.leavePeriod().endDate());
        assertEquals(original.reason(), restored.reason());
        assertEquals(original.leaveType(), restored.leaveType());
        assertEquals(original.status(), restored.status());
    }

    @Test
    @DisplayName("Trimmed domain values survive persistence mapping")
    void trimmedDomainValuesSurviveMapping() {
        LeaveRequest original = LeaveRequest.create(
                REQUEST_ID,
                "  staff-member-id  ",
                "  manager-id  ",
                LEAVE_PERIOD,
                "  Family holiday  ",
                LeaveType.ANNUAL
        );

        LeaveRequest restored = LeaveRequestJpaToDomainMapper.map(
                LeaveRequestDomainToJpaMapper.map(original)
        );

        assertEquals("staff-member-id", restored.staffMemberId());
        assertEquals("manager-id", restored.managerId());
        assertEquals("Family holiday", restored.reason());
    }

    @Test
    @DisplayName("A null domain request cannot be mapped to persistence")
    void nullDomainRequestIsRejected() {
        Throwable exception = assertThrows(NullPointerException.class, () ->
                LeaveRequestDomainToJpaMapper.map(null)
        );

        assertEquals(LeaveRequestDomainToJpaMapper.LEAVE_REQUEST_NOT_NULL, exception.getMessage());
    }

    @Test
    @DisplayName("A null persistence request cannot be mapped to the domain")
    void nullJpaRequestIsRejected() {
        Throwable exception = assertThrows(NullPointerException.class, () ->
                LeaveRequestJpaToDomainMapper.map(null)
        );

        assertEquals(LeaveRequestJpaToDomainMapper.LEAVE_REQUEST_JPA_NOT_NULL, exception.getMessage());
    }
}
