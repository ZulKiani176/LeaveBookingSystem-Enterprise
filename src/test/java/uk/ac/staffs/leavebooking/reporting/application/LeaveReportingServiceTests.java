package uk.ac.staffs.leavebooking.reporting.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestApprovedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestSubmittedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.SickLeaveRecordedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveDayPortion;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveStatus;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveType;
import uk.ac.staffs.leavebooking.reporting.application.dto.LeaveReportingDTO;
import uk.ac.staffs.leavebooking.reporting.infrastructure.entities.LeaveReportingProjectionJpa;
import uk.ac.staffs.leavebooking.reporting.infrastructure.repositories.LeaveReportingProjectionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Leave Reporting Service")
class LeaveReportingServiceTests {
    private static final LocalDate DATE = LocalDate.parse("2026-08-24");

    @Mock private LeaveReportingProjectionRepository repository;

    @Test
    @DisplayName("Submitted leave creates a privacy-safe reporting projection")
    void submittedLeaveCreatesProjection() {
        LeaveReportingService service = service();
        when(repository.findById("request-1")).thenReturn(Optional.empty());

        service.apply(submitted(10L));

        LeaveReportingProjectionJpa saved = captureSaved();
        assertEquals(10L, saved.getSourceEventId());
        assertEquals(IntegrationLeaveStatus.PENDING, saved.getStatus());
        assertEquals(new BigDecimal("0.5"), saved.getChargedLeaveDays());
        assertFalse(List.of(LeaveReportingProjectionJpa.class.getDeclaredFields()).stream()
                .anyMatch(field -> field.getName().equals("reason")));
    }

    @Test
    @DisplayName("A later lifecycle event advances the same projection")
    void laterEventAdvancesProjection() {
        LeaveReportingService service = service();
        LeaveReportingProjectionJpa existing = projection(10L, IntegrationLeaveStatus.PENDING);
        when(repository.findById("request-1")).thenReturn(Optional.of(existing));

        service.apply(approved(11L));

        assertEquals(11L, existing.getSourceEventId());
        assertEquals(IntegrationLeaveStatus.APPROVED, existing.getStatus());
        verify(repository).save(existing);
    }

    @Test
    @DisplayName("Duplicate and older deliveries are idempotently ignored")
    void duplicateAndOlderDeliveriesAreIgnored() {
        LeaveReportingService service = service();
        LeaveReportingProjectionJpa existing = projection(11L, IntegrationLeaveStatus.APPROVED);
        when(repository.findById("request-1")).thenReturn(Optional.of(existing));

        service.apply(approved(11L));
        service.apply(submitted(10L));

        verify(repository, never()).save(any());
        assertEquals(IntegrationLeaveStatus.APPROVED, existing.getStatus());
        assertEquals(11L, existing.getSourceEventId());
    }

    @Test
    @DisplayName("Sickness is projected with zero charge and no private reason")
    void sicknessIsProjectedWithoutReason() {
        LeaveReportingService service = service();
        when(repository.findById("sick-1")).thenReturn(Optional.empty());

        service.apply(new SickLeaveRecordedIntegrationEvent(
                12L, DATE, "sick-1", "staff-1", "manager-1",
                DATE, DATE.plusDays(2), IntegrationLeaveType.SICK,
                IntegrationLeaveDayPortion.FULL_DAY,
                BigDecimal.ZERO, IntegrationLeaveStatus.RECORDED
        ));

        LeaveReportingProjectionJpa saved = captureSaved();
        assertEquals(IntegrationLeaveType.SICK, saved.getLeaveType());
        assertEquals(IntegrationLeaveStatus.RECORDED, saved.getStatus());
        assertEquals(0, saved.getChargedLeaveDays().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Reporting query filters by optional leave type and status")
    void reportingQueryFiltersProjectionResults() {
        LeaveReportingService service = service();
        when(repository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDate(
                DATE.plusDays(7), DATE.minusDays(7)
        )).thenReturn(List.of(
                projection(10L, IntegrationLeaveStatus.PENDING),
                new LeaveReportingProjectionJpa(
                        "sick-1", 12L, "staff-2", IntegrationLeaveType.SICK,
                        DATE, DATE.plusDays(2), IntegrationLeaveDayPortion.FULL_DAY,
                        BigDecimal.ZERO, IntegrationLeaveStatus.RECORDED, DATE
                )
        ));

        List<LeaveReportingDTO> result = service.find(
                DATE.minusDays(7), DATE.plusDays(7),
                IntegrationLeaveType.SICK, IntegrationLeaveStatus.RECORDED
        );

        assertEquals(1, result.size());
        assertEquals("sick-1", result.getFirst().leaveRequestId());
    }

    @Test
    @DisplayName("A reversed reporting range is rejected before persistence")
    void reversedReportingRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                service().find(DATE, DATE.minusDays(1), null, null)
        );

        verify(repository, never())
                .findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDate(
                        any(), any()
                );
    }

    private LeaveReportingProjectionJpa captureSaved() {
        ArgumentCaptor<LeaveReportingProjectionJpa> captor =
                ArgumentCaptor.forClass(LeaveReportingProjectionJpa.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private LeaveReportingProjectionJpa projection(
            Long sourceId,
            IntegrationLeaveStatus status
    ) {
        return new LeaveReportingProjectionJpa(
                "request-1", sourceId, "staff-1", IntegrationLeaveType.ANNUAL,
                DATE, DATE, IntegrationLeaveDayPortion.MORNING,
                new BigDecimal("0.5"), status, DATE
        );
    }

    private LeaveRequestSubmittedIntegrationEvent submitted(Long id) {
        return new LeaveRequestSubmittedIntegrationEvent(
                id, DATE, "request-1", "staff-1", "manager-1",
                DATE, DATE, IntegrationLeaveType.ANNUAL,
                IntegrationLeaveDayPortion.MORNING,
                new BigDecimal("0.5"), IntegrationLeaveStatus.PENDING
        );
    }

    private LeaveRequestApprovedIntegrationEvent approved(Long id) {
        return new LeaveRequestApprovedIntegrationEvent(
                id, DATE, "request-1", "staff-1", DATE, DATE,
                IntegrationLeaveType.ANNUAL, IntegrationLeaveDayPortion.MORNING,
                new BigDecimal("0.5"), IntegrationLeaveStatus.APPROVED
        );
    }

    private LeaveReportingService service() {
        return new LeaveReportingService(repository);
    }
}
